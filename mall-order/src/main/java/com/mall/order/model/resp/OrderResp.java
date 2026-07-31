package com.mall.order.model.resp;

import com.mall.order.entity.Order;
import com.mall.order.entity.OrderItem;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class OrderResp {
    private Order order;
    private List<OrderItem> items;
}
