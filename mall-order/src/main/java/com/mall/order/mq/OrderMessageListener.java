package com.mall.order.mq;

import com.alibaba.fastjson.JSON;
import com.mall.order.mq.dto.PayResultMessage;
import com.mall.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
@RocketMQMessageListener(topic = "order-pay-result", consumerGroup = "order-consumer-group")
public class OrderMessageListener implements RocketMQListener<String> {

    @Resource
    private OrderService orderService;

    @Override
    public void onMessage(String message) {
        log.info("Received payment result: {}", message);
        try {
            PayResultMessage msg = JSON.parseObject(message, PayResultMessage.class);
            String orderNo = msg.getOrderNo();
            Integer status = msg.getStatus();

            if (orderNo == null) {
                log.warn("Invalid payment message: missing orderNo");
                return;
            }

            if (status == 1) {
                // 支付成功 → 幂等处理（paySuccess 内部已做状态检查）
                orderService.paySuccess(orderNo);
                log.info("Order {} paid via MQ", orderNo);
            } else if (status == 6) {
                // 退款 → 更新订单状态为已退款 + 回滚库存（幂等处理）
                orderService.refundOrder(orderNo);
                log.info("Order {} refunded via MQ", orderNo);
            } else {
                log.warn("Unknown payment status {} for order {}", status, orderNo);
            }
        } catch (Exception e) {
            log.error("Failed to process payment message: {}", message, e);
        }
    }
}
