package com.mall.order.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.constant.RedisKey;
import com.mall.common.exception.BizException;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderItem;
import com.mall.order.feign.ProductFeignClient;
import com.mall.order.feign.UserFeignClient;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.model.req.CreateOrderReq;
import com.mall.order.model.resp.OrderResp;
import com.mall.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderItemMapper orderItemMapper;
    @Resource
    private ProductFeignClient productFeignClient;
    @Resource
    private UserFeignClient userFeignClient;
    @Resource
    private RedissonClient redissonClient;

    @Override
    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    @Transactional
    public OrderResp createOrder(Long userId, CreateOrderReq req) {
        // 防重复下单：基于用户ID+商品列表生成幂等键
        String itemsStr = req.getItems().toString();
        String hash = md5(itemsStr);
        String idempotentKey = "order:idempotent:" + userId + ":" + hash;
        org.redisson.api.RBucket<Object> bucket = redissonClient.getBucket(idempotentKey);
        boolean firstAttempt = bucket.trySet("1", 60, TimeUnit.SECONDS);
        if (!firstAttempt) {
            throw new BizException("请勿重复提交订单");
        }
        try {
            return doCreateOrder(userId, req);
        } catch (Exception e) {
            bucket.delete();
            throw e;
        }
    }

    private OrderResp doCreateOrder(Long userId, CreateOrderReq req) {
        // Build order
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setStatus(0); // PENDING_PAY
        order.setRemark(req.getRemark());
        order.setFreightAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);

        // Get real address info from user service
        try {
            com.mall.common.entity.Result<?> addrResult = userFeignClient.getAddress(req.getAddressId());
            if (addrResult != null && addrResult.getData() != null) {
                java.util.Map<String, Object> rawAddr = (java.util.Map<String, Object>) addrResult.getData();
                order.setReceiverName((String) rawAddr.get("receiverName"));
                order.setReceiverPhone((String) rawAddr.get("receiverPhone"));
                String fullAddr = String.format("%s%s%s%s",
                        rawAddr.get("province") != null ? rawAddr.get("province") : "",
                        rawAddr.get("city") != null ? rawAddr.get("city") : "",
                        rawAddr.get("district") != null ? rawAddr.get("district") : "",
                        rawAddr.get("detailAddress") != null ? rawAddr.get("detailAddress") : "");
                order.setReceiverAddress(fullAddr);
            } else {
                throw new BizException("收货地址获取失败");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get address from user service", e);
            throw new BizException("收货地址获取失败");
        }

        // Process each item with Redisson distributed lock for stock deduction
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        // 记录已成功扣库存的商品，用于部分失败时回滚
        List<CreateOrderReq.OrderItemReq> deductedItems = new ArrayList<>();

        for (CreateOrderReq.OrderItemReq itemReq : req.getItems()) {
            String lockKey = RedisKey.LOCK_PREFIX + "stock:" + itemReq.getProductId();
            RLock lock = redissonClient.getLock(lockKey);

            try {
                boolean locked = lock.tryLock(5, 10, TimeUnit.SECONDS);
                if (!locked) {
                    throw new BizException("系统繁忙，请稍后再试");
                }

                // Get real product info from product service
                com.mall.common.entity.Result<?> prodResult = productFeignClient.getProductDetail(itemReq.getProductId());
                if (prodResult == null || prodResult.getData() == null) {
                    throw new BizException("商品信息获取失败");
                }
                java.util.Map<String, Object> productMap = (java.util.Map<String, Object>) prodResult.getData();
                String productName = (String) productMap.get("name");
                String productImage = (String) productMap.get("mainImage");
                BigDecimal productPrice = new BigDecimal(productMap.get("price").toString());

                // Deduct stock via Feign
                try {
                    productFeignClient.updateStock(itemReq.getProductId(), -itemReq.getQuantity());
                } catch (Exception e) {
                    log.error("Failed to deduct stock for product {}", itemReq.getProductId(), e);
                    throw new BizException("商品库存扣减失败");
                }

                // 记录已扣减的商品，用于部分失败时回滚
                deductedItems.add(itemReq);

                // Create order item
                OrderItem item = new OrderItem();
                item.setOrderId(order.getId());
                item.setOrderNo(order.getOrderNo());
                item.setProductId(itemReq.getProductId());
                item.setProductName(productName);
                item.setProductImage(productImage);
                item.setProductPrice(productPrice);
                item.setQuantity(itemReq.getQuantity());
                item.setTotalPrice(productPrice.multiply(new BigDecimal(itemReq.getQuantity())));
                orderItems.add(item);

                totalAmount = totalAmount.add(item.getTotalPrice());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // 部分扣减成功，回滚已扣库存
                rollbackDeducted(deductedItems);
                throw new BizException("系统异常");
            } catch (Exception e) {
                // 部分扣减成功，回滚已扣库存
                rollbackDeducted(deductedItems);
                throw e;
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);

        orderMapper.insert(order);

        // Save order items
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        // Send delay message: auto-cancel after 30min if not paid
        // In production: rocketMQ.sendDelayMessage(order, 30min)

        log.info("Order created: {} for user {}, amount: {}", order.getOrderNo(), userId, totalAmount);

        return OrderResp.builder()
                .order(order)
                .items(orderItems)
                .build();
    }

    @Override
    public Order getOrder(Long id) {
        return orderMapper.selectById(id);
    }

    @Override
    public OrderResp getOrderDetail(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BizException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new BizException("无权查看此订单");

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));

        return OrderResp.builder().order(order).items(items).build();
    }

    @Override
    public Page<Order> pageOrders(Long userId, int pageNum, int pageSize, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(userId != null, Order::getUserId, userId)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional
    public void paySuccess(String orderNo) {
        Order order = getByOrderNo(orderNo);
        if (order == null || order.getStatus() != 0) {
            log.warn("Order {} not found or status is not PENDING_PAY", orderNo);
            return;
        }

        order.setStatus(1); // PAID
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // Increment sales for each product in this order
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, orderNo));
        for (OrderItem item : items) {
            try {
                productFeignClient.incrementSales(item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                log.error("Failed to increment sales for product {} in order {}", item.getProductId(), orderNo, e);
            }
        }

        log.info("Order {} paid successfully, sales updated for {} items", orderNo, items.size());

        // TODO: Send MQ message to distribution service for commission calculation
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new BizException("无权操作此订单");
        if (order.getStatus() != 0) throw new BizException("订单状态不允许取消");

        order.setStatus(4); // CANCELLED
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason);
        orderMapper.updateById(order);

        // Rollback stock with retry (失败则抛异常回滚取消操作)
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        rollbackStock(items);

        log.info("Order {} cancelled: {}", order.getOrderNo(), reason);
    }

    @Override
    @Transactional
    public void cancelUnpaidOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        List<Order> unpaidOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, 0)
                .le(Order::getCreateTime, threshold));

        for (Order order : unpaidOrders) {
            order.setStatus(4);
            order.setCancelTime(LocalDateTime.now());
            order.setCancelReason("超时未支付，系统自动取消");
            orderMapper.updateById(order);

            // Rollback stock with retry
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            try {
                rollbackStock(items);
            } catch (Exception e) {
                log.error("Failed to rollback stock for order {}, will retry later", order.getOrderNo(), e);
            }
        }

        log.info("Cancelled {} unpaid orders", unpaidOrders.size());
    }

    @Override
    @Transactional
    public void cancelByOrderNo(String orderNo, String reason) {
        Order order = getByOrderNo(orderNo);
        if (order == null) {
            log.warn("Order {} not found for cancelByOrderNo", orderNo);
            return;
        }
        if (order.getStatus() != 0) {
            log.warn("Order {} status is {}, cannot cancel", orderNo, order.getStatus());
            return;
        }
        order.setStatus(4);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason);
        orderMapper.updateById(order);

        // Rollback stock
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderNo, orderNo));
        rollbackStock(items);
        log.info("Order {} cancelled by orderNo: {}", orderNo, reason);
    }

    @Override
    public Order getByOrderNo(String orderNo) {
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
    }

    @Override
    @Transactional
    public void confirmReceive(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new BizException("无权操作此订单");
        if (order.getStatus() != 2) throw new BizException("订单状态不允许确认收货");

        order.setStatus(3); // COMPLETED
        order.setCompleteTime(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("Order {} confirmed received by user {}", order.getOrderNo(), userId);

        // TODO: Send MQ to distribution service for commission settlement
    }

    private String generateOrderNo() {
        return IdUtil.getSnowflakeNextIdStr();
    }

    private String md5(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
    }

    /**
     * 回滚库存，最多重试3次
     */
    private void rollbackStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            retryRollback(item.getProductId(), item.getQuantity());
        }
    }

    private void retryRollback(Long productId, int quantity) {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                productFeignClient.updateStock(productId, quantity);
                return;
            } catch (Exception e) {
                log.warn("Rollback stock failed for product {}, attempt {}/{}", productId, i + 1, maxRetries, e);
                if (i < maxRetries - 1) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BizException("系统异常");
                    }
                }
            }
        }
        throw new BizException("库存回滚失败，请联系客服");
    }

    /**
     * 多商品下单时，部分商品扣库存失败后回滚已成功的商品
     */
    private void rollbackDeducted(List<CreateOrderReq.OrderItemReq> deductedItems) {
        for (CreateOrderReq.OrderItemReq item : deductedItems) {
            try {
                productFeignClient.updateStock(item.getProductId(), item.getQuantity());
                log.info("Rolled back stock for product {}: +{}", item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                log.error("Rollback failed for product {}, manual intervention may be needed", item.getProductId(), e);
            }
        }
    }
}
