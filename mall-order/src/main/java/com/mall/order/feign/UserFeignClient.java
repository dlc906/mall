package com.mall.order.feign;

import com.mall.common.entity.Result;
import com.mall.order.feign.dto.AddressDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "mall-user", path = "/api/user")
public interface UserFeignClient {

    @GetMapping("/address/{id}")
    Result<AddressDTO> getAddress(@PathVariable("id") Long id);
}
