package com.aichuangzuo.user.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 解析管理端 JWT，用于用户端内部接口鉴权。
 */
@Component
public class AdminJwtUtil {

    /**
     * 管理端 JWT Secret，必须通过环境变量注入，禁止硬编码。
     * 长度不少于 256 位（32 字节）。
     */
    @Value("${auth.jwt.admin-access-secret}")
    private String adminAccessSecret;

    @PostConstruct
    public void validateSecret() {
        if (adminAccessSecret == null || adminAccessSecret.isBlank()) {
            throw new IllegalStateException("配置缺失：auth.jwt.admin-access-secret 必须配置，且长度不少于 256 位（32 字节）");
        }
        if (adminAccessSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("配置错误：auth.jwt.admin-access-secret 长度必须不少于 256 位（32 字节）");
        }
    }

    public Long parseAccessToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(adminAccessSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (ExpiredJwtException e) {
            throw new JwtException("admin token expired", e);
        } catch (JwtException e) {
            throw new JwtException("invalid admin token", e);
        }
    }
}
