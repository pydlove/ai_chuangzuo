package com.aichuangzuo.user.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 解析管理端 JWT，用于用户端内部接口鉴权。
 */
@Component
public class AdminJwtUtil {

    @Value("${auth.jwt.admin-access-secret:please-change-this-admin-access-secret-at-least-256-bits-long}")
    private String adminAccessSecret;

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
