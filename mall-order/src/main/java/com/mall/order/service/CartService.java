package com.mall.order.service;

import com.mall.order.model.resp.CartItemResp;

import java.util.List;

public interface CartService {
    /**
     * 添加商品到购物车（相同商品合并数量）
     */
    void addItem(Long userId, Long productId, Integer quantity);

    /**
     * 获取购物车列表
     */
    List<CartItemResp> listItems(Long userId);

    /**
     * 更新商品数量
     */
    void updateQuantity(Long userId, Long productId, Integer quantity);

    /**
     * 切换选中状态
     */
    void toggleCheck(Long userId, Long productId, Boolean checked);

    /**
     * 全选/全不选
     */
    void checkAll(Long userId, Boolean checked);

    /**
     * 删除购物车商品
     */
    void removeItem(Long userId, Long productId);

    /**
     * 清空购物车
     */
    void clearCart(Long userId);
}
