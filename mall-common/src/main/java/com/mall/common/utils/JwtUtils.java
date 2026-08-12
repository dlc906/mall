package com.mall.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
public class JwtUtils {
    /**
     * 签名密钥：优先从环境变量 JWT_SECRET 读取（生产环境必须设置），
     * 未设置时回退到内置默认值（仅限本地开发）。
     * 注意：HS256 要求密钥至少 32 字节，自定义密钥时请保证长度。
     */
    private static final String SECRET = System.getenv().getOrDefault(
            "JWT_SECRET",
            "mall-secret-key-2024-this-is-a-very-long-secret-key-for-jwt");
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    /** AccessToken 过期时间: 30分钟 */
    public static final long ACCESS_TOKEN_EXPIRE = 30 * 60 * 1000L;
    /** RefreshToken 过期时间: 7天 */
    public static final long REFRESH_TOKEN_EXPIRE = 7 * 24 * 60 * 60 * 1000L;

    public static String createAccessToken(Long userId, String username) {
        return createToken(userId, username, ACCESS_TOKEN_EXPIRE);
    }

    public static String createRefreshToken(Long userId, String username) {
        return createToken(userId, username, REFRESH_TOKEN_EXPIRE);
    }

    private static String createToken(Long userId, String username, long expireTime) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireTime);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("type", expireTime == ACCESS_TOKEN_EXPIRE ? "access" : "refresh")
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 严格解析：token 无效或已过期都返回 null。
     * 用于网关鉴权、refresh 等需要"过期即拒绝"的场景。
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            log.warn("JWT解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 宽容解析：token 过期也返回 claims（仅用于登出清理等场景）。
     */
    public static Claims parseTokenAllowExpired(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 过期 token：返回 claims 以便登出时清理用户状态
            return e.getClaims();
        } catch (JwtException e) {
            log.warn("JWT解析失败: {}", e.getMessage());
            return null;
        }
    }

    public static boolean isTokenExpired(String token) {
        Claims claims = parseTokenAllowExpired(token);
        if (claims == null) return true;
        return claims.getExpiration().before(new Date());
    }

    /**
     * 严格获取 userId：token 无效或过期返回 null。
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 宽容获取 userId：过期 token 也能取到（用于登出清理）。
     */
    public static Long getUserIdAllowExpired(String token) {
        Claims claims = parseTokenAllowExpired(token);
        if (claims == null) return null;
        return Long.valueOf(claims.getSubject());
    }

    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        return claims.get("username", String.class);
    }
}
