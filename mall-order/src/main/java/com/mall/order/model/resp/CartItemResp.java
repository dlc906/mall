package com.mall.order.model.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResp {
    private Long productId;
    private String name;
    private String image;
    private BigDecimal price;
    private Integer stock;
    private Integer quantity;
    private Boolean checked;
}
