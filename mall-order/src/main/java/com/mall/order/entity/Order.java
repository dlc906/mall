package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mall_order")
public class Order extends BaseEntity {
    private String orderNo;
    private Long userId;
    private Integer status;        // 0=待支付, 1=已支付, 2=已发货, 3=已完成, 4=已取消, 5=退款中, 6=已退款
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private BigDecimal discountAmount;
    private BigDecimal freightAmount;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String remark;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime completeTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
}
