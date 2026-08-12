package com.mall.order.feign.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 商品服务返回的商品信息 DTO（Feign 传输对象）
 */
@Data
public class ProductDTO {
    private Long id;
    private String name;
    private String mainImage;
    private BigDecimal price;
    private Integer stock;
}
