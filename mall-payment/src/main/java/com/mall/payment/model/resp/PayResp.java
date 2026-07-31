package com.mall.payment.model.resp;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PayResp {
    private String paymentNo;
    private String orderNo;
    private BigDecimal amount;
    private Integer status;
    private Boolean success;
}
