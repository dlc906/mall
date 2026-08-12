package com.mall.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.mall.common.constant.CommonConstants;
import com.mall.common.constant.RedisKey;
import com.mall.common.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /** 白名单路径 - 不需要认证 */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/user/register",
            "/api/user/verify-code",
            "/api/product/list",
            "/api/product/detail",
            "/api/product/search",
            "/api/product/category",
            "/doc.html",
            "/webjars",
            "/v3/api-docs",
            "/swagger-resources"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单直接放行
        if (isWhitePath(path)) {
            return chain.filter(exchange);
        }

        // 获取Token
        String authHeader = request.getHeaders().getFirst(CommonConstants.AUTH_HEADER);
        if (StrUtil.isBlank(authHeader) || !authHeader.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return unauthorized(exchange, "未登录或Token已过期");
        }

        String token = authHeader.substring(CommonConstants.TOKEN_PREFIX.length());

        // 检查Token是否在黑名单(已登出)
        String blacklistKey = RedisKey.TOKEN_BLACKLIST + token;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            return unauthorized(exchange, "Token已失效，请重新登录");
        }

        // 校验Token（严格解析：无效或过期返回null）
        io.jsonwebtoken.Claims claims = JwtUtils.parseToken(token);
        if (claims == null) {
            return unauthorized(exchange, "Token无效或已过期");
        }

        // 只允许 access token 访问业务接口，防止 refresh token（7天）被当作 access 使用
        String tokenType = claims.get("type", String.class);
        if (!"access".equals(tokenType)) {
            return unauthorized(exchange, "Token类型错误，请使用AccessToken");
        }

        Long userId = Long.valueOf(claims.getSubject());

        // 将用户信息传递给下游服务
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-Username", String.valueOf(claims.get("username")))
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * 白名单路径段匹配：
     * 精确匹配白名单路径本身，或匹配其下子路径（如 /api/product/detail/1）。
     * 不使用纯 startsWith，避免 /api/user/verify-code-xxx 等前缀相似路径被误放行。
     */
    private boolean isWhitePath(String path) {
        return WHITE_LIST.stream().anyMatch(white ->
                path.equals(white) || path.startsWith(white + "/"));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"code\":401,\"message\":\"%s\",\"data\":null}", message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
