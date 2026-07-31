package com.mall.order.controller;

import com.mall.common.entity.Result;
import com.mall.order.model.req.CartAddReq;
import com.mall.order.model.req.CartUpdateReq;
import com.mall.order.model.resp.CartItemResp;
import com.mall.order.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Tag(name = "购物车管理", description = "购物车增删改查（Redis持久化）")
@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Resource
    private CartService cartService;

    @Operation(summary = "添加商品到购物车")
    @PostMapping("/add")
    public Result<Void> addItem(@RequestHeader("X-User-Id") Long userId,
                                 @Valid @RequestBody CartAddReq req) {
        cartService.addItem(userId, req.getProductId(), req.getQuantity());
        return Result.success();
    }

    @Operation(summary = "获取购物车列表")
    @GetMapping("/list")
    public Result<List<CartItemResp>> listItems(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(cartService.listItems(userId));
    }

    @Operation(summary = "更新商品数量")
    @PutMapping("/quantity")
    public Result<Void> updateQuantity(@RequestHeader("X-User-Id") Long userId,
                                        @Valid @RequestBody CartUpdateReq req) {
        cartService.updateQuantity(userId, req.getProductId(), req.getQuantity());
        return Result.success();
    }

    @Operation(summary = "切换选中状态")
    @PutMapping("/check")
    public Result<Void> toggleCheck(@RequestHeader("X-User-Id") Long userId,
                                     @Valid @RequestBody CartUpdateReq req) {
        cartService.toggleCheck(userId, req.getProductId(), req.getChecked());
        return Result.success();
    }

    @Operation(summary = "全选/全不选")
    @PutMapping("/checkAll")
    public Result<Void> checkAll(@RequestHeader("X-User-Id") Long userId,
                                  @RequestParam Boolean checked) {
        cartService.checkAll(userId, checked);
        return Result.success();
    }

    @Operation(summary = "删除购物车商品")
    @DeleteMapping("/{productId}")
    public Result<Void> removeItem(@RequestHeader("X-User-Id") Long userId,
                                    @PathVariable Long productId) {
        cartService.removeItem(userId, productId);
        return Result.success();
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping("/clear")
    public Result<Void> clearCart(@RequestHeader("X-User-Id") Long userId) {
        cartService.clearCart(userId);
        return Result.success();
    }
}
