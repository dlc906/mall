package com.mall.job.feign;

import com.mall.common.entity.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "mall-order", path = "/api/order")
public interface OrderFeignClient {

    @PostMapping("/cancel-unpaid")
    Result<Void> cancelUnpaidOrders();
}
