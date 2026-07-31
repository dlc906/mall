package com.mall.product.model.req;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class ProductReq {
    @NotBlank(message = "商品名称不能为空")
    private String name;
    @NotNull(message = "分类不能为空")
    private Long categoryId;
    private String title;
    private String description;
    private String mainImage;
    private String images;
    private String detail;
    @NotNull(message = "价格不能为空")
    private BigDecimal price;
    private BigDecimal originalPrice;
    @NotNull(message = "库存不能为空")
    private Integer stock;
    private Integer status;
    private Integer sort;
    private String keywords;
    private String specs;
}
