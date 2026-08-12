package com.mall.payment.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mall.common.entity.Result;
import com.mall.payment.entity.PaymentRecord;
import com.mall.payment.feign.OrderFeignClient;
import com.mall.payment.mapper.PaymentRecordMapper;
import com.mall.payment.mq.PaymentProducer;
import com.mall.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 支付补偿定时任务
 * 
 * 职责：
 * 1. 扫描 status=0（处理中）且超过 3 分钟未完成的支付记录
 * 2. 查询订单服务实际状态
 * 3. 订单已支付 → 同步本地记录为成功
 * 4. 订单未支付 → 重新投递 MQ 消息
 * 5. 重试超过 3 次 → 标记失败并自动退款（Saga 补偿）
 */
@Slf4j
@Component
public class PaymentRetryTask {

    @Resource
    private PaymentRecordMapper paymentRecordMapper;
    @Resource
    private OrderFeignClient orderFeignClient;
    @Resource
    private PaymentProducer paymentProducer;
    @Resource
    private PaymentService paymentService;

    /** 超时阈值：创建超过 3 分钟仍未完成的记录 */
    private static final long TIMEOUT_MINUTES = 3;

    /** 最大重试次数 */
    private static final int MAX_RETRIES = 3;

    @Scheduled(fixedDelay = 30_000)  // 每 30 秒执行一次
    public void retryPendingPayments() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

        List<PaymentRecord> pendingList = paymentRecordMapper.selectList(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getStatus, 0)
                        .eq(PaymentRecord::getPayType, 0)
                        .lt(PaymentRecord::getCreateTime, deadline));

        if (pendingList.isEmpty()) {
            return;
        }

        log.info("PaymentRetryTask: found {} pending payment records to process", pendingList.size());

        for (PaymentRecord record : pendingList) {
            try {
                processPendingRecord(record);
            } catch (Exception e) {
                log.error("PaymentRetryTask: error processing paymentNo={}", record.getPaymentNo(), e);
            }
        }
    }

    /**
     * 退款补偿：扫描已退款（payType=1, status=1）但订单服务可能未同步的记录，
     * 确保订单状态已更新（1→6）且库存已回滚。
     */
    @Scheduled(fixedDelay = 30_000)  // 每 30 秒执行一次
    public void retryPendingRefunds() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

        List<PaymentRecord> pendingRefunds = paymentRecordMapper.selectList(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getStatus, 1)
                        .eq(PaymentRecord::getPayType, 1)
                        .lt(PaymentRecord::getCreateTime, deadline));

        if (pendingRefunds.isEmpty()) {
            return;
        }

        log.info("PaymentRetryTask: found {} pending refund records to process", pendingRefunds.size());

        for (PaymentRecord record : pendingRefunds) {
            try {
                processPendingRefund(record);
            } catch (Exception e) {
                log.error("PaymentRetryTask: error processing refund paymentNo={}", record.getPaymentNo(), e);
            }
        }
    }

    /**
     * 处理单条退款记录：确保订单服务已同步（状态1→6 + 库存回滚）
     */
    private void processPendingRefund(PaymentRecord record) {
        int currentRetry = record.getRetryCount() != null ? record.getRetryCount() : 0;

        // 查询订单服务实际状态
        Integer orderStatus = queryOrderStatus(record.getOrderNo());
        if (orderStatus == null) {
            // 订单服务不可用，跳过本轮，等下一轮
            log.warn("PaymentRetryTask: order service unavailable for refund orderNo={}, skip", record.getOrderNo());
            return;
        }

        if (orderStatus == 6) {
            // 订单已退款，流程完成
            log.info("PaymentRetryTask: refund already applied for orderNo={}", record.getOrderNo());
            return;
        }

        // 订单未退款（仍是已支付或其他状态），重试同步
        int nextRetry = currentRetry + 1;
        if (nextRetry > MAX_RETRIES) {
            // 重试超限：标记人工介入
            paymentRecordMapper.update(null, new LambdaUpdateWrapper<PaymentRecord>()
                    .eq(PaymentRecord::getPaymentNo, record.getPaymentNo())
                    .set(PaymentRecord::getRemark, "退款通知重试超限，需人工介入（订单状态=" + orderStatus + "）")
                    .set(PaymentRecord::getLastRetryTime, LocalDateTime.now()));
            log.error("PaymentRetryTask: refund retry exhausted for orderNo={}, MANUAL INTERVENTION REQUIRED",
                    record.getOrderNo());
        } else {
            // 重试：同步调用退款 + MQ 兜底
            paymentService.updateRetryInfo(record.getPaymentNo(), nextRetry);
            try {
                com.mall.common.entity.Result<Void> result = orderFeignClient.refundOrder(record.getOrderNo());
                if (result == null || !result.isSuccess()) {
                    paymentProducer.sendPayResult(record.getOrderNo(), 6);
                }
            } catch (Exception e) {
                log.warn("PaymentRetryTask: sync refund failed for orderNo={}, send MQ", record.getOrderNo());
                paymentProducer.sendPayResult(record.getOrderNo(), 6);
            }
            log.info("PaymentRetryTask: retried refund for orderNo={}, retry={}/{}",
                    record.getOrderNo(), nextRetry, MAX_RETRIES);
        }
    }

    /**
     * 处理单条超时未完成的支付记录
     */
    private void processPendingRecord(PaymentRecord record) {
        int currentRetry = record.getRetryCount() != null ? record.getRetryCount() : 0;

        // 步骤1：查询订单服务的实际状态
        Integer orderStatus = queryOrderStatus(record.getOrderNo());

        if (orderStatus == null) {
            // 订单服务不可用，跳过本轮重试，等下一轮
            log.warn("PaymentRetryTask: order service unavailable for orderNo={}, skip", record.getOrderNo());
            return;
        }

        if (orderStatus == 1) {
            // 步骤2：订单已支付 → 同步本地记录
            log.info("PaymentRetryTask: order {} already paid, syncing local record", record.getOrderNo());
            paymentService.updateRetryInfo(record.getPaymentNo(), currentRetry + 1);
            // 标记为成功
            paymentRecordMapper.update(null, new LambdaUpdateWrapper<PaymentRecord>()
                    .eq(PaymentRecord::getPaymentNo, record.getPaymentNo())
                    .eq(PaymentRecord::getStatus, 0)
                    .set(PaymentRecord::getStatus, 1)
                    .set(PaymentRecord::getRemark, "支付成功（定时补偿）")
                    .set(PaymentRecord::getLastRetryTime, LocalDateTime.now()));
            return;
        }

        // 步骤3：订单未支付 → 判断重试次数
        int nextRetry = currentRetry + 1;
        if (nextRetry > MAX_RETRIES) {
            // 步骤4：重试超限 → Saga 补偿：标记失败 + 自动退款
            log.warn("PaymentRetryTask: retry exhausted for orderNo={}, initiating auto refund", record.getOrderNo());
            paymentService.markPaymentFailed(record.getPaymentNo(),
                    "支付通知重试超限，自动退款", nextRetry);
            // 发起退款（调用订单服务的退款接口）
            try {
                orderFeignClient.cancelOrderByOrderNo(record.getOrderNo(), "支付超时自动退款");
            } catch (Exception e) {
                log.error("PaymentRetryTask: auto refund failed for orderNo={}", record.getOrderNo(), e);
            }
        } else {
            // 重试：更新重试信息 + 重新投递 MQ
            paymentService.updateRetryInfo(record.getPaymentNo(), nextRetry);
            paymentProducer.sendPayResult(record.getOrderNo(), 1);
            log.info("PaymentRetryTask: resent MQ for orderNo={}, retry={}/{}",
                    record.getOrderNo(), nextRetry, MAX_RETRIES);
        }
    }

    /**
     * 查询订单服务的实际状态
     * @return 1=已支付, 0=待支付, null=查询失败
     */
    private Integer queryOrderStatus(String orderNo) {
        try {
            Result<com.mall.payment.feign.dto.OrderDTO> result = orderFeignClient.getOrderByOrderNo(orderNo);
            if (result == null || result.getData() == null) {
                return null;
            }
            return result.getData().getStatus();
        } catch (Exception e) {
            log.warn("PaymentRetryTask: failed to query order status for orderNo={}", orderNo, e);
            return null;
        }
    }
}
