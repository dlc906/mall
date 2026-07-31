package com.mall.common.constant;

public interface CommonConstants {
    /** 成功标记 */
    Integer SUCCESS = 200;
    /** 错误标记 */
    Integer ERROR = 500;
    /** 未授权 */
    Integer UNAUTHORIZED = 401;
    /** 禁止访问 */
    Integer FORBIDDEN = 403;
    /** 未找到 */
    Integer NOT_FOUND = 404;

    /** Token 前缀 */
    String TOKEN_PREFIX = "Bearer ";
    /** Token Header */
    String AUTH_HEADER = "Authorization";
    /** 刷新 Token 头部 */
    String REFRESH_TOKEN_HEADER = "X-Refresh-Token";

    /** 删除标记 */
    Integer DELETED = 1;
    /** 未删除 */
    Integer NOT_DELETED = 0;

    /** 启用 */
    Integer ENABLE = 1;
    /** 禁用 */
    Integer DISABLE = 0;
}
