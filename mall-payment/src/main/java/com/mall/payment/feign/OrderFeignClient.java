package com.mall.payment.feign;

import com.mall.common.entity.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mall-order", path = "/api/order")
public interface OrderFeignClient {

    @GetMapping("/no/{orderNo}")
    Result<?> getOrderByOrderNo(@PathVariable("orderNo") String orderNo);

    @PutMapping("/pay-success/{orderNo}")
    Result<Void> paySuccess(@PathVariable("orderNo") String orderNo);
    
    @PutMapping("/cancel-by-no/{orderNo}")
    Result<Void> cancelOrderByOrderNo(@PathVariable("orderNo") String orderNo,
                                       @RequestParam("reason") String reason);
}
