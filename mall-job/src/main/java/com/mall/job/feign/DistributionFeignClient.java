package com.mall.job.feign;

import com.mall.common.entity.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "mall-distribution", path = "/api/distribution")
public interface DistributionFeignClient {

    @PostMapping("/settle")
    Result<Void> settleCommission();
}
