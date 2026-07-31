package com.mall.job.feign;

import com.mall.common.entity.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "mall-product", path = "/api/product")
public interface ProductFeignClient {

    @PostMapping("/sync-es")
    Result<Void> syncToEs();
}
