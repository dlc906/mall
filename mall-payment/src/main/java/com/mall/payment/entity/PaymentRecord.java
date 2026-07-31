package com.mall.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_payment_record")
public class PaymentRecord extends BaseEntity {
    private String paymentNo;     // 支付单号
    private String orderNo;       // 订单号
    private Long userId;
    private BigDecimal amount;
    private Integer payType;      // 0=支付, 1=退款
    private Integer payMethod;    // 1=模拟支付, 2=微信, 3=支付宝
    private Integer status;       // 0=处理中, 1=成功, 2=失败
    private String tradeNo;       // 第三方交易号
    private LocalDateTime payTime;
    private Integer retryCount;   // 已重试次数
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastRetryTime;  // 最后重试时间
    private String remark;
}
