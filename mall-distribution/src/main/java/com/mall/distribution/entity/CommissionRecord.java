package com.mall.distribution.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_commission_record")
public class CommissionRecord extends BaseEntity {
    private Long userId;           // 获得佣金的分销用户ID
    private String orderNo;        // 关联订单号
    private Long orderId;          // 关联订单ID
    private Long buyerUserId;      // 下单用户ID
    private BigDecimal orderAmount;// 订单金额
    private BigDecimal commissionRatio; // 佣金比例(%)
    private BigDecimal commissionAmount; // 佣金金额
    private Integer level;         // 1=一级佣金, 2=二级佣金
    private Integer status;        // 0=待结算, 1=已结算, 2=已提现
    private String settleMonth;    // 结算月份 2024-01
}
