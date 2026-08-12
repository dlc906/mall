package com.mall.order.feign;

import com.mall.common.entity.Result;
import com.mall.order.feign.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "mall-product", path = "/api/product")
public interface ProductFeignClient {

    @GetMapping("/detail/{id}")
    Result<ProductDTO> getProductDetail(@PathVariable("id") Long id);

    @PutMapping("/stock/{id}")
    Result<Void> updateStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);

    @PutMapping("/sales/{id}")
    Result<Void> incrementSales(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);
}
