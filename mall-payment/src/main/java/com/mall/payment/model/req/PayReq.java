package com.mall.payment.model.req;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class PayReq {
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotNull(message = "支付金额不能为空")
    private BigDecimal amount;

    private Integer payMethod; // 1=模拟支付
}
