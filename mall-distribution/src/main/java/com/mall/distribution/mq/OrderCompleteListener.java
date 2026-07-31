package com.mall.distribution.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mall.distribution.service.DistributionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
@RocketMQMessageListener(topic = "order-completed", consumerGroup = "distribution-consumer-group")
public class OrderCompleteListener implements RocketMQListener<String> {

    @Resource
    private DistributionService distributionService;

    @Override
    public void onMessage(String message) {
        log.info("Received order completed event: {}", message);
        try {
            JSONObject obj = JSON.parseObject(message);
            Long orderId = obj.getLong("orderId");
            String orderNo = obj.getString("orderNo");
            Long userId = obj.getLong("userId");
            // BigDecimal orderAmount = obj.getBigDecimal("orderAmount");

            distributionService.calculateCommission(orderId, orderNo, userId);
        } catch (Exception e) {
            log.error("Failed to process order completion for distribution", e);
        }
    }
}
