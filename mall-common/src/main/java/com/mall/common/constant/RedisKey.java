package com.mall.common.constant;

public interface RedisKey {
    /** 用户Token前缀 */
    String USER_TOKEN = "mall:user:token:";
    /** 用户RefreshToken前缀 */
    String USER_REFRESH_TOKEN = "mall:user:refresh:";
    /** Token黑名单前缀 */
    String TOKEN_BLACKLIST = "mall:token:blacklist:";
    /** 商品库存缓存 */
    String PRODUCT_STOCK = "mall:product:stock:";
    /** 商品详情缓存 */
    String PRODUCT_INFO = "mall:product:info:";
    /** 限流前缀 */
    String RATE_LIMIT = "mall:rate:limit:";
    /** 分布式锁前缀 */
    String LOCK_PREFIX = "mall:lock:";
    /** 购物车前缀 */
    String CART = "mall:cart:";
    /** 验证码前缀 */
    String VERIFY_CODE = "mall:verify:code:";
}
