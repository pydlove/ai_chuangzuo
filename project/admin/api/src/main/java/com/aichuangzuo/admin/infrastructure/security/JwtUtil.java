package com.aichuangzuo.admin.infrastructure.security;

import com.aichuangzuo.shared.enums.error.AdminAuthErrorCode;
import com.aichuangzuo.shared.exception.UnauthorizedException;
import com.aichuangzuo.admin.config.AuthProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final AuthProperties authProperties;

    public String generateAccessToken(Long adminUserId) {
        return generateToken(adminUserId, authProperties.getJwt().getAccessSecret(),
                authProperties.getJwt().getAccessExpiration() * 1000);
    }

    public String generateRefreshToken(Long adminUserId) {
        return generateToken(adminUserId, authProperties.getJwt().getRefreshSecret(),
                authProperties.getJwt().getRefreshExpiration() * 1000);
    }

    private String generateToken(Long adminUserId, String secret, long expirationMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(String.valueOf(adminUserId))
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Long parseAccessToken(String token) {
        return parseToken(token, authProperties.getJwt().getAccessSecret());
    }

    public Long parseRefreshToken(String token) {
        return parseToken(token, authProperties.getJwt().getRefreshSecret());
    }

    private Long parseToken(String token, String secret) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException(AdminAuthErrorCode.TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new UnauthorizedException(AdminAuthErrorCode.TOKEN_EXPIRED);
        }
    }

    public String getJti(String token) {
        SecretKey key = Keys.hmacShaKeyFor(authProperties.getJwt().getAccessSecret().getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getId();
    }

    public Date getExpiration(String token) {
        SecretKey key = Keys.hmacShaKeyFor(authProperties.getJwt().getAccessSecret().getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getExpiration();
    }
}
