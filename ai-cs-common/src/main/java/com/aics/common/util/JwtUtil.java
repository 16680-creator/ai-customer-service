package com.aics.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 */
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    /** 默认密钥（生产环境应从配置中心读取） */
    private static final String DEFAULT_SECRET = "aics-platform-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256";

    /** 默认过期时间：24小时 */
    private static final long DEFAULT_EXPIRATION = 24 * 60 * 60 * 1000L;

    private JwtUtil() {
    }

    /**
     * 生成 JWT Token
     *
     * @param subject  主题（通常为用户ID）
     * @param claims   自声明
     * @param secret   密钥
     * @param expirationMillis 过期时间（毫秒）
     * @return token 字符串
     */
    public static String generateToken(String subject, Map<String, Object> claims, String secret, long expirationMillis) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 生成 JWT Token（使用默认密钥和过期时间）
     *
     * @param subject 主题（通常为用户ID）
     * @param claims  自声明
     * @return token 字符串
     */
    public static String generateToken(String subject, Map<String, Object> claims) {
        return generateToken(subject, claims, DEFAULT_SECRET, DEFAULT_EXPIRATION);
    }

    /**
     * 解析 Token
     *
     * @param token  JWT Token
     * @param secret 密钥
     * @return Claims
     */
    public static Claims parseToken(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 解析 Token（使用默认密钥）
     *
     * @param token JWT Token
     * @return Claims
     */
    public static Claims parseToken(String token) {
        return parseToken(token, DEFAULT_SECRET);
    }

    /**
     * 从 Token 中获取 Subject（用户ID）
     *
     * @param token  JWT Token
     * @param secret 密钥
     * @return subject
     */
    public static String getSubject(String token, String secret) {
        return parseToken(token, secret).getSubject();
    }

    /**
     * 从 Token 中获取 Subject（使用默认密钥）
     *
     * @param token JWT Token
     * @return subject
     */
    public static String getSubject(String token) {
        return getSubject(token, DEFAULT_SECRET);
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token  JWT Token
     * @param secret 密钥
     * @return 是否有效
     */
    public static boolean validateToken(String token, String secret) {
        try {
            parseToken(token, secret);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token 已过期: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("JWT Token 无效: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证 Token 是否有效（使用默认密钥）
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public static boolean validateToken(String token) {
        return validateToken(token, DEFAULT_SECRET);
    }

    /**
     * 判断 Token 是否即将过期（剩余时间小于指定阈值）
     *
     * @param token           JWT Token
     * @param secret          密钥
     * @param thresholdMillis 阈值（毫秒）
     * @return 是否即将过期
     */
    public static boolean isTokenExpiringSoon(String token, String secret, long thresholdMillis) {
        try {
            Claims claims = parseToken(token, secret);
            Date expiration = claims.getExpiration();
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return remaining < thresholdMillis;
        } catch (Exception e) {
            return true;
        }
    }
}
