package com.mall.common.utils;

import cn.hutool.core.date.DateUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Slf4j
public class JwtUtils {
    private static final String SECRET = "mall-secret-key-2024-this-is-a-very-long-secret-key-for-jwt";
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

    public static Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (JwtException e) {
            log.warn("JWT解析失败: {}", e.getMessage());
            return null;
        }
    }

    public static boolean isTokenExpired(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return true;
        return claims.getExpiration().before(new Date());
    }

    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        return Long.valueOf(claims.getSubject());
    }

    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        return claims.get("username", String.class);
    }
}
