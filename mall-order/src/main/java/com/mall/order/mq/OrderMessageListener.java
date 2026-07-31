package com.mall.order.mq;

import com.alibaba.fastjson.JSON;
import com.mall.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

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
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = JSON.parseObject(message, Map.class);
            String orderNo = (String) msg.get("orderNo");
            Integer status = msg.get("status") instanceof Integer
                    ? (Integer) msg.get("status")
                    : Integer.valueOf(msg.get("status").toString());

            if (orderNo == null) {
                log.warn("Invalid payment message: missing orderNo");
                return;
            }

            if (status == 1) {
                // 支付成功 → 幂等处理（paySuccess 内部已做状态检查）
                orderService.paySuccess(orderNo);
                log.info("Order {} paid via MQ", orderNo);
            } else if (status == 6) {
                // 退款 → 调用取消订单（保留扩展）
                log.info("Order {} refund via MQ, processing...", orderNo);
            } else {
                log.warn("Unknown payment status {} for order {}", status, orderNo);
            }
        } catch (Exception e) {
            log.error("Failed to process payment message: {}", message, e);
        }
    }
}
