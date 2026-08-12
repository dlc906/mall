package com.mall.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderItem;
import com.mall.order.model.req.CreateOrderReq;
import com.mall.order.model.resp.OrderResp;
import java.util.List;

public interface OrderService {
    OrderResp createOrder(Long userId, CreateOrderReq req);
    Order getOrder(Long id);
    OrderResp getOrderDetail(Long userId, Long id);
    Page<Order> pageOrders(Long userId, int pageNum, int pageSize, Integer status);
    void paySuccess(String orderNo);
    void cancelOrder(Long userId, Long orderId, String reason);
    void cancelUnpaidOrders();
    void cancelByOrderNo(String orderNo, String reason);
    void refundOrder(String orderNo);
    Order getByOrderNo(String orderNo);
    void confirmReceive(Long userId, Long orderId);
}
