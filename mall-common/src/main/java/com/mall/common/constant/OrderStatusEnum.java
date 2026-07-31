package com.mall.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatusEnum {
    PENDING_PAY(0, "待支付"),
    PAID(1, "已支付"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消"),
    REFUNDING(5, "退款中"),
    REFUNDED(6, "已退款");

    private final Integer code;
    private final String desc;

    public static String getDesc(Integer code) {
        for (OrderStatusEnum e : values()) {
            if (e.code.equals(code)) return e.desc;
        }
        return "";
    }
}
