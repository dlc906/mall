package com.mall.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.entity.Result;
import com.mall.product.entity.Product;
import com.mall.product.model.es.ProductDocument;
import com.mall.product.model.req.ProductReq;
import com.mall.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Tag(name = "商品管理", description = "商品CRUD、ES搜索")
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Resource
    private ProductService productService;

    @Operation(summary = "商品列表")
    @GetMapping("/list")
    public Result<Page<Product>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return Result.success(productService.pageProducts(pageNum, pageSize, categoryId, keyword));
    }

    @Operation(summary = "ES搜索商品")
    @GetMapping("/search")
    public Result<Page<ProductDocument>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(productService.searchFromEs(keyword, categoryId, pageNum, pageSize));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/detail/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.success(productService.getProductDetail(id));
    }

    @Operation(summary = "新增商品(管理端)")
    @PostMapping
    public Result<Product> add(@Valid @RequestBody ProductReq req) {
        return Result.success(productService.addProduct(req));
    }

    @Operation(summary = "修改商品(管理端)")
    @PutMapping("/{id}")
    public Result<Product> update(@PathVariable Long id, @Valid @RequestBody ProductReq req) {
        return Result.success(productService.updateProduct(id, req));
    }

    @Operation(summary = "删除商品(管理端)")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success();
    }

    @Operation(summary = "扣减库存(内部Feign调用)")
    @PutMapping("/stock/{id}")
    public Result<Void> updateStock(@PathVariable Long id, @RequestParam int quantity) {
        productService.updateStock(id, quantity);
        return Result.success();
    }

    @Operation(summary = "增加销量(内部Feign调用)")
    @PutMapping("/sales/{id}")
    public Result<Void> incrementSales(@PathVariable Long id, @RequestParam int quantity) {
        productService.incrementSales(id, quantity);
        return Result.success();
    }

    @Operation(summary = "同步所有商品到ES(管理端)")
    @PostMapping("/sync-es")
    public Result<Void> syncEs() {
        productService.syncAllToEs();
        return Result.success();
    }
}
