package com.mall.distribution.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_distribution_relationship")
public class DistributionRelationship extends BaseEntity {
    private Long userId;         // 下级用户ID
    private Long parentId;       // 上级(一级分销)用户ID
    private Long grandparentId;  // 上上级(二级分销)用户ID
    private Integer level;       // 1=一级分销, 2=二级分销
}
