package com.mall.order.model.req;

import lombok.Data;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class CreateOrderReq {
    @NotNull(message = "地址不能为空")
    private Long addressId;

    @NotEmpty(message = "订单项不能为空")
    private List<OrderItemReq> items;

    private String remark;

    @Data
    public static class OrderItemReq {
        @NotNull(message = "商品ID不能为空")
        private Long productId;
        @NotNull(message = "数量不能为空")
        private Integer quantity;
    }
}
