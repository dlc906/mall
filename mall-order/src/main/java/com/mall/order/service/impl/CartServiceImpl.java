package com.mall.order.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.constant.RedisKey;
import com.mall.common.exception.BizException;
import com.mall.order.feign.ProductFeignClient;
import com.mall.order.model.resp.CartItemResp;
import com.mall.order.service.CartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ProductFeignClient productFeignClient;

    @Resource
    private ObjectMapper objectMapper;

    private String cartKey(Long userId) {
        return RedisKey.CART + userId;
    }

    @Override
    public void addItem(Long userId, Long productId, Integer quantity) {
        String key = cartKey(userId);
        String field = productId.toString();

        Object existRaw = redisTemplate.opsForHash().get(key, field);
        if (existRaw != null) {
            // 已存在，合并数量
            CartItemResp existItem = convertValue(existRaw, CartItemResp.class);
            existItem.setQuantity(existItem.getQuantity() + quantity);
            if (existItem.getStock() != null && existItem.getQuantity() > existItem.getStock()) {
                existItem.setQuantity(existItem.getStock());
            }
            redisTemplate.opsForHash().put(key, field, existItem);
            log.info("Cart add(merge): userId={}, productId={}, quantity={}", userId, productId, existItem.getQuantity());
        } else {
            // 不存在，从商品服务获取商品信息
            CartItemResp newItem = fetchProductInfo(productId);
            newItem.setQuantity(quantity);
            newItem.setChecked(true);
            redisTemplate.opsForHash().put(key, field, newItem);
            log.info("Cart add(new): userId={}, productId={}, quantity={}", userId, productId, quantity);
        }
    }

    @Override
    public List<CartItemResp> listItems(Long userId) {
        String key = cartKey(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        return entries.values().stream()
                .map(v -> {
                    CartItemResp item = convertValue(v, CartItemResp.class);
                    // 刷新实时库存
                    refreshStock(item);
                    return item;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void updateQuantity(Long userId, Long productId, Integer quantity) {
        String key = cartKey(userId);
        String field = productId.toString();
        Object raw = redisTemplate.opsForHash().get(key, field);
        if (raw == null) {
            throw new BizException("购物车中不存在该商品");
        }
        CartItemResp item = convertValue(raw, CartItemResp.class);
        if (item.getStock() != null && quantity > item.getStock()) {
            quantity = item.getStock();
        }
        item.setQuantity(quantity);
        redisTemplate.opsForHash().put(key, field, item);
        log.info("Cart updateQuantity: userId={}, productId={}, quantity={}", userId, productId, quantity);
    }

    @Override
    public void toggleCheck(Long userId, Long productId, Boolean checked) {
        String key = cartKey(userId);
        String field = productId.toString();
        Object raw = redisTemplate.opsForHash().get(key, field);
        if (raw == null) {
            throw new BizException("购物车中不存在该商品");
        }
        CartItemResp item = convertValue(raw, CartItemResp.class);
        item.setChecked(checked);
        redisTemplate.opsForHash().put(key, field, item);
    }

    @Override
    public void checkAll(Long userId, Boolean checked) {
        String key = cartKey(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            CartItemResp item = convertValue(entry.getValue(), CartItemResp.class);
            item.setChecked(checked);
            redisTemplate.opsForHash().put(key, entry.getKey().toString(), item);
        }
    }

    @Override
    public void removeItem(Long userId, Long productId) {
        String key = cartKey(userId);
        redisTemplate.opsForHash().delete(key, productId.toString());
        log.info("Cart remove: userId={}, productId={}", userId, productId);
    }

    @Override
    public void clearCart(Long userId) {
        String key = cartKey(userId);
        redisTemplate.delete(key);
        log.info("Cart clear: userId={}", userId);
    }

    /**
     * 使用 Jackson ObjectMapper 安全转换（兼容 GenericJackson2JsonRedisSerializer 的 @class 格式）
     */
    private CartItemResp convertValue(Object raw, Class<CartItemResp> clazz) {
        if (raw instanceof CartItemResp) {
            return (CartItemResp) raw;
        }
        // GenericJackson2JsonRedisSerializer 可能已反序列化为 Map 或 LinkedHashMap
        // 也可能存为纯字符串（兼容旧数据）
        try {
            if (raw instanceof String) {
                return objectMapper.readValue((String) raw, clazz);
            }
            // Map / LinkedHashMap 等情况
            return objectMapper.convertValue(raw, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cart item: {}", raw, e);
            throw new BizException("购物车数据异常");
        }
    }

    /**
     * 从商品服务刷新实时库存（仅刷新返回值中的库存，不同步回 Redis）
     */
    private void refreshStock(CartItemResp item) {
        try {
            com.mall.common.entity.Result<?> result = productFeignClient.getProductDetail(item.getProductId());
            if (result != null && result.getData() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) result.getData();
                Integer currentStock = map.get("stock") != null
                        ? Integer.valueOf(map.get("stock").toString())
                        : 0;
                item.setStock(currentStock);
                if (item.getQuantity() > currentStock) {
                    item.setQuantity(currentStock);
                }
                log.debug("Cart stock refreshed: productId={}, stock={}", item.getProductId(), currentStock);
            }
        } catch (Exception e) {
            log.warn("Failed to refresh stock for productId={}, use cached value", item.getProductId());
        }
    }

    private CartItemResp fetchProductInfo(Long productId) {
        try {
            com.mall.common.entity.Result<?> result = productFeignClient.getProductDetail(productId);
            if (result == null || result.getData() == null) {
                throw new BizException("商品信息获取失败");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result.getData();
            return CartItemResp.builder()
                    .productId(productId)
                    .name((String) map.get("name"))
                    .image((String) map.get("mainImage"))
                    .price(new BigDecimal(map.get("price").toString()))
                    .stock(map.get("stock") != null ? Integer.valueOf(map.get("stock").toString()) : 0)
                    .quantity(0)
                    .checked(true)
                    .build();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch product info for productId={}", productId, e);
            throw new BizException("商品信息获取失败");
        }
    }
}
