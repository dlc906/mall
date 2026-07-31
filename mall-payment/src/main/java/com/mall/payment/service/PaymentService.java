package com.mall.payment.service;

import com.mall.payment.model.req.PayReq;
import com.mall.payment.model.resp.PayResp;
import com.mall.payment.entity.PaymentRecord;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface PaymentService {
    PayResp pay(Long userId, PayReq req);
    PaymentRecord getPayment(String paymentNo);
    Page<PaymentRecord> pagePayments(Long userId, int pageNum, int pageSize);
    PayResp refund(Long userId, String orderNo);

    /** 供定时任务调用：标记支付失败 + 记录重试次数 */
    void markPaymentFailed(String paymentNo, String reason, int retryCount);
    /** 供定时任务调用：更新重试信息 */
    void updateRetryInfo(String paymentNo, int retryCount);
}
