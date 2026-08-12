package com.mall.order.mq.dto;

import lombok.Data;

/**
 * 支付结果 MQ 消息体（由 mall-payment 发送）
 */
@Data
public class PayResultMessage {
    private String orderNo;
    private Integer status;   // 1=支付成功, 6=退款
    private Long timestamp;
}
