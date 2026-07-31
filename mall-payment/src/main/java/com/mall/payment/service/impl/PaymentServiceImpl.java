package com.mall.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.exception.BizException;
import com.mall.payment.entity.PaymentRecord;
import com.mall.payment.feign.OrderFeignClient;
import com.mall.payment.mapper.PaymentRecordMapper;
import com.mall.payment.model.req.PayReq;
import com.mall.payment.model.resp.PayResp;
import com.mall.payment.mq.PaymentProducer;
import com.mall.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Resource
    private PaymentRecordMapper paymentRecordMapper;
    @Resource
    private OrderFeignClient orderFeignClient;
    @Resource
    private PaymentProducer paymentProducer;
    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public PayResp pay(Long userId, PayReq req) {
        // ========== 阶段一：本地事务（创建支付记录 status=0） ==========
        PaymentRecord record = transactionTemplate.execute((TransactionCallback<PaymentRecord>) status -> {
            try {
                return doCreatePendingPayment(userId, req);
            } catch (BizException e) {
                status.setRollbackOnly();
                throw e;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw new BizException("支付创建失败");
            }
        });

        // ========== 阶段二：事务外同步通知订单服务 ==========
        boolean syncSuccess = false;
        try {
            com.mall.common.entity.Result<Void> result = orderFeignClient.paySuccess(req.getOrderNo());
            if (result != null && result.isSuccess()) {
                syncSuccess = true;
            }
        } catch (Exception e) {
            log.error("Sync paySuccess failed for orderNo={}, will rely on MQ", req.getOrderNo(), e);
        }

        if (syncSuccess) {
            // 同步成功 → 更新本地记录为成功
            transactionTemplate.execute(status -> {
                doMarkPaymentSuccess(record.getPaymentNo());
                return null;
            });
            log.info("Payment sync success: paymentNo={}, orderNo={}", record.getPaymentNo(), req.getOrderNo());
        } else {
            // 同步失败 → MQ 兜底（本地记录保持 status=0，由定时任务补偿）
            paymentProducer.sendPayResult(req.getOrderNo(), 1);
            log.warn("Payment sync failed, MQ sent for retry: orderNo={}", req.getOrderNo());
        }

        return PayResp.builder()
                .paymentNo(record.getPaymentNo())
                .orderNo(record.getOrderNo())
                .amount(record.getAmount())
                .status(syncSuccess ? 1 : 0)
                .success(syncSuccess)
                .build();
    }

    /**
     * 校验 + 创建支付记录（在事务内执行）
     */
    private PaymentRecord doCreatePendingPayment(Long userId, PayReq req) {
        // 校验订单存在且属于当前用户
        // 注意：订单服务不可用时跳过校验（留待定时任务补偿），不阻止支付流程
        boolean orderVerified = false;
        try {
            com.mall.common.entity.Result<?> result = orderFeignClient.getOrderByOrderNo(req.getOrderNo());
            if (result == null || result.getData() == null) {
                throw new BizException("订单不存在");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> orderMap = (Map<String, Object>) result.getData();
            Object orderUserId = orderMap.get("userId");
            if (orderUserId == null || !orderUserId.toString().equals(userId.toString())) {
                throw new BizException("无权支付此订单");
            }
            Object orderStatus = orderMap.get("status");
            if (orderStatus != null && !"0".equals(orderStatus.toString())) {
                throw new BizException("订单状态不允许支付");
            }
            orderVerified = true;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            // 订单服务不可达（宕机/网络故障）：跳过校验，留待定时任务补偿
            log.warn("Order service unavailable, skip order verification for orderNo={}, will retry later",
                    req.getOrderNo());
        }

        // 幂等检查：防止重复支付
        Long count = paymentRecordMapper.selectCount(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderNo, req.getOrderNo())
                .eq(PaymentRecord::getPayType, 0)
                .eq(PaymentRecord::getStatus, 1));
        if (count > 0) {
            throw new BizException("该订单已支付");
        }

        // 创建支付记录（status=0 处理中）
        PaymentRecord record = new PaymentRecord();
        record.setPaymentNo(IdUtil.getSnowflakeNextIdStr());
        record.setOrderNo(req.getOrderNo());
        record.setUserId(userId);
        record.setAmount(req.getAmount());
        record.setPayType(0);
        record.setPayMethod(req.getPayMethod() != null ? req.getPayMethod() : 1);
        record.setStatus(0);  // 处理中
        record.setTradeNo("SIM-" + System.currentTimeMillis());
        record.setPayTime(LocalDateTime.now());
        record.setRetryCount(0);
        record.setRemark("支付处理中");

        try {
            paymentRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            throw new BizException("该订单已支付");
        }

        log.info("Payment record created(pending): paymentNo={}, orderNo={}",
                record.getPaymentNo(), req.getOrderNo());
        return record;
    }

    /**
     * 标记支付成功（在事务内执行）
     */
    private void doMarkPaymentSuccess(String paymentNo) {
        PaymentRecord record = paymentRecordMapper.selectOne(
                new LambdaQueryWrapper<PaymentRecord>().eq(PaymentRecord::getPaymentNo, paymentNo));
        if (record == null || record.getStatus() != 0) {
            return; // 幂等
        }
        record.setStatus(1);
        record.setRemark("支付成功");
        paymentRecordMapper.updateById(record);
        log.info("Payment record marked success: paymentNo={}", paymentNo);
    }

    /**
     * 标记支付失败 + 增加重试次数（在事务内执行，供定时任务调用）
     */
    public void markPaymentFailed(String paymentNo, String reason, int retryCount) {
        transactionTemplate.execute(status -> {
            PaymentRecord record = paymentRecordMapper.selectOne(
                    new LambdaQueryWrapper<PaymentRecord>().eq(PaymentRecord::getPaymentNo, paymentNo));
            if (record == null || record.getStatus() != 0) {
                return null;
            }
            record.setStatus(2);
            record.setRetryCount(retryCount);
            record.setLastRetryTime(LocalDateTime.now());
            record.setRemark(reason);
            paymentRecordMapper.updateById(record);
            log.warn("Payment record marked failed: paymentNo={}, reason={}", paymentNo, reason);
            return null;
        });
    }

    /**
     * 更新重试信息（在事务内执行，供定时任务调用）
     */
    public void updateRetryInfo(String paymentNo, int retryCount) {
        transactionTemplate.execute(status -> {
            PaymentRecord record = paymentRecordMapper.selectOne(
                    new LambdaQueryWrapper<PaymentRecord>().eq(PaymentRecord::getPaymentNo, paymentNo));
            if (record == null) {
                return null;
            }
            record.setRetryCount(retryCount);
            record.setLastRetryTime(LocalDateTime.now());
            paymentRecordMapper.updateById(record);
            return null;
        });
    }

    @Override
    public PaymentRecord getPayment(String paymentNo) {
        return paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getPaymentNo, paymentNo));
    }

    @Override
    public Page<PaymentRecord> pagePayments(Long userId, int pageNum, int pageSize) {
        return paymentRecordMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getUserId, userId)
                        .orderByDesc(PaymentRecord::getCreateTime));
    }

    @Override
    public PayResp refund(Long userId, String orderNo) {
        return transactionTemplate.execute(status -> {
            try {
                return doRefund(userId, orderNo);
            } catch (BizException e) {
                status.setRollbackOnly();
                throw e;
            }
        });
    }

    private PayResp doRefund(Long userId, String orderNo) {
        PaymentRecord payRecord = paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderNo, orderNo)
                .eq(PaymentRecord::getPayType, 0)
                .eq(PaymentRecord::getStatus, 1));

        if (payRecord == null) {
            throw new BizException("未找到原始支付记录");
        }

        Long count = paymentRecordMapper.selectCount(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderNo, orderNo)
                .eq(PaymentRecord::getPayType, 1)
                .eq(PaymentRecord::getStatus, 1));
        if (count > 0) {
            throw new BizException("该订单已退款");
        }

        PaymentRecord refundRecord = new PaymentRecord();
        refundRecord.setPaymentNo(IdUtil.getSnowflakeNextIdStr());
        refundRecord.setOrderNo(orderNo);
        refundRecord.setUserId(userId);
        refundRecord.setAmount(payRecord.getAmount());
        refundRecord.setPayType(1);
        refundRecord.setPayMethod(payRecord.getPayMethod());
        refundRecord.setStatus(1);
        refundRecord.setTradeNo("REFUND-" + System.currentTimeMillis());
        refundRecord.setPayTime(LocalDateTime.now());
        refundRecord.setRemark("模拟退款成功");

        try {
            paymentRecordMapper.insert(refundRecord);
        } catch (DuplicateKeyException e) {
            throw new BizException("该订单已退款");
        }

        paymentProducer.sendPayResult(orderNo, 6);

        return PayResp.builder()
                .paymentNo(refundRecord.getPaymentNo())
                .orderNo(orderNo)
                .amount(refundRecord.getAmount())
                .status(1)
                .success(true)
                .build();
    }
}
