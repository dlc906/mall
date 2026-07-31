package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_product")
public class Product extends BaseEntity {
    private String name;
    private Long categoryId;
    private String title;
    private String description;
    private String mainImage;
    private String images;         // JSON array of image URLs
    private String detail;         // Rich text HTML
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer sales;
    private Integer status;        // 0=下架, 1=上架
    private Integer sort;
    private String keywords;       // for ES search
    private String specs;          // JSON: [{"name":"颜色","values":["红","蓝"]}]
}
