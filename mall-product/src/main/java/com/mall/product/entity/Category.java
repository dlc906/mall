package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_category")
public class Category extends BaseEntity {
    private String name;
    private Long parentId;
    private Integer level;  // 1=一级, 2=二级, 3=三级
    private Integer sort;
    private String icon;
    private Integer status;
}
