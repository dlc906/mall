package com.mall.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.constant.OrderStatusEnum;
import com.mall.common.entity.Result;
import com.mall.order.entity.Order;
import com.mall.order.model.req.CreateOrderReq;
import com.mall.order.model.resp.OrderResp;
import com.mall.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "订单管理", description = "创建订单、查询订单、取消订单")
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Resource
    private OrderService orderService;

    @Operation(summary = "创建订单")
    @PostMapping
    public Result<OrderResp> createOrder(@RequestHeader("X-User-Id") Long userId,
                                          @Valid @RequestBody CreateOrderReq req) {
        return Result.success(orderService.createOrder(userId, req));
    }

    @Operation(summary = "订单列表")
    @GetMapping("/list")
    public Result<Page<Order>> list(@RequestHeader("X-User-Id") Long userId,
                                     @RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "10") int pageSize,
                                     @RequestParam(required = false) Integer status) {
        return Result.success(orderService.pageOrders(userId, pageNum, pageSize, status));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderResp> detail(@RequestHeader("X-User-Id") Long userId,
                                     @PathVariable Long id) {
        return Result.success(orderService.getOrderDetail(userId, id));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@RequestHeader("X-User-Id") Long userId,
                                @PathVariable Long id,
                                @RequestParam(defaultValue = "用户取消") String reason) {
        orderService.cancelOrder(userId, id, reason);
        return Result.success();
    }

    @Operation(summary = "确认收货")
    @PostMapping("/{id}/confirm")
    public Result<Void> confirmReceive(@RequestHeader("X-User-Id") Long userId,
                                        @PathVariable Long id) {
        orderService.confirmReceive(userId, id);
        return Result.success();
    }

    @Operation(summary = "取消超时未支付订单(管理/XXL-Job)")
    @PostMapping("/cancel-unpaid")
    public Result<Void> cancelUnpaid() {
        orderService.cancelUnpaidOrders();
        return Result.success();
    }

    @Operation(summary = "根据订单号取消订单(内部Feign/Saga补偿)")
    @PutMapping("/cancel-by-no/{orderNo}")
    public Result<Void> cancelByOrderNo(@PathVariable String orderNo,
                                         @RequestParam(defaultValue = "系统取消") String reason) {
        orderService.cancelByOrderNo(orderNo, reason);
        return Result.success();
    }

    @Operation(summary = "根据订单号查询(内部Feign)")
    @GetMapping("/no/{orderNo}")
    public Result<Order> getByOrderNo(@PathVariable String orderNo) {
        return Result.success(orderService.getByOrderNo(orderNo));
    }

    @Operation(summary = "支付成功回调(内部Feign)")
    @PutMapping("/pay-success/{orderNo}")
    public Result<Void> paySuccess(@PathVariable String orderNo) {
        orderService.paySuccess(orderNo);
        return Result.success();
    }

    @Operation(summary = "退款回调(内部Feign)")
    @PutMapping("/refund/{orderNo}")
    public Result<Void> refund(@PathVariable String orderNo) {
        orderService.refundOrder(orderNo);
        return Result.success();
    }

    @Operation(summary = "获取订单状态枚举")
    @GetMapping("/statuses")
    public Result<Map<Integer, String>> getStatuses() {
        Map<Integer, String> map = new HashMap<>();
        for (OrderStatusEnum e : OrderStatusEnum.values()) {
            map.put(e.getCode(), e.getDesc());
        }
        return Result.success(map);
    }
}
