package com.mall.payment.mq;

import com.alibaba.fastjson.JSON;
import com.mall.payment.entity.PaymentRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class PaymentProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public void sendPayResult(String orderNo, Integer status) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("orderNo", orderNo);
        msg.put("status", status);
        msg.put("timestamp", System.currentTimeMillis());

        rocketMQTemplate.convertAndSend("order-pay-result", JSON.toJSONString(msg));
        log.info("Sent payment result: orderNo={}, status={}", orderNo, status);
    }
}
