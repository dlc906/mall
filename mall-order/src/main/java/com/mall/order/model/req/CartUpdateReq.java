package com.mall.order.model.req;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class CartUpdateReq {
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Min(value = 1, message = "数量最少为1")
    @Max(value = 999, message = "数量不能超过999")
    private Integer quantity;

    private Boolean checked;
}
