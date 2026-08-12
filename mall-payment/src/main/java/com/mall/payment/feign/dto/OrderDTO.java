package com.mall.payment.feign.dto;

import lombok.Data;

/**
 * 订单服务返回的订单信息 DTO（Feign 传输对象）
 */
@Data
public class OrderDTO {
    private Long id;
    private String orderNo;
    private Long userId;
    private Integer status;   // 0=待支付, 1=已支付, ...
}
