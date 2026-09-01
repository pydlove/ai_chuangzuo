package com.aichuangzuo.user.infrastructure.security;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.UnauthorizedException;
import com.aichuangzuo.user.config.AuthProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final String REMEMBER_ME_CLAIM = "remember_me";
    private static final String EXPORT_BIZ_NO_CLAIM = "biz_no";

    private final AuthProperties authProperties;

    public String generateAccessToken(Long userId) {
        return generateToken(userId, authProperties.getJwt().getAccessSecret(),
                authProperties.getJwt().getAccessExpiration() * 1000, null);
    }

    public String generateRefreshToken(Long userId, boolean rememberMe) {
        long expirationSeconds = rememberMe
                ? authProperties.getJwt().getRememberMeRefreshExpiration()
                : authProperties.getJwt().getRefreshExpiration();
        Map<String, Object> claims = new HashMap<>();
        claims.put(REMEMBER_ME_CLAIM, rememberMe);
        return generateToken(userId, authProperties.getJwt().getRefreshSecret(),
                expirationSeconds * 1000, claims);
    }

    private String generateToken(Long userId, String secret, long expirationMillis,
                                 Map<String, Object> extraClaims) {
        Date now = Date.from(Instant.now());
        Date expiry = new Date(now.getTime() + expirationMillis);
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry);
        if (extraClaims != null) {
            extraClaims.forEach(builder::claim);
        }
        return builder.signWith(key).compact();
    }

    public Long parseAccessToken(String token) {
        return parseToken(token, authProperties.getJwt().getAccessSecret());
    }

    public Long parseRefreshToken(String token) {
        return parseToken(token, authProperties.getJwt().getRefreshSecret());
    }

    public String generateExportToken(String bizNo) {
        long expirationMillis = authProperties.getJwt().getExportExpiration() * 1000;
        Map<String, Object> claims = new HashMap<>();
        claims.put(EXPORT_BIZ_NO_CLAIM, bizNo);
        return generateToken(0L, authProperties.getJwt().getExportSecret(), expirationMillis, claims);
    }

    public String parseExportToken(String token) {
        Claims claims = parseExportClaims(token);
        Object bizNo = claims.get(EXPORT_BIZ_NO_CLAIM);
        if (bizNo == null) {
            throw new UnauthorizedException(UserAuthErrorCode.TOKEN_INVALID);
        }
        return bizNo.toString();
    }

    private Claims parseExportClaims(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(authProperties.getJwt().getExportSecret().getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException(UserAuthErrorCode.TOKEN_EXPIRED);
        } catch (SignatureException e) {
            throw new UnauthorizedException(UserAuthErrorCode.TOKEN_INVALID);
        } catch (MalformedJwtException e) {
            throw new UnauthorizedException(UserAuthErrorCode.TOKEN_INVALID);
        } catch (UnsupportedJwtException e) {
            throw new UnauthorizedException(UserAuthErrorCode.TOKEN_INVALID);
        } catch (JwtException e) {
            throw new UnauthorizedException(UserAuthErrorCode.TOKEN_INVALID);
        }
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
            throw new UnauthorizedException(UserAuthErrorCode.TOKEN_EXPIRED);
        } catch (SignatureException e) {
            throw new UnauthorizedException(UserAuthErrorCode.TOKEN_INVALID);
        } catch (MalformedJwtException e) {
            throw new UnauthorizedException(UserAuthErrorCode.TOKEN_INVALID);
        } catch (UnsupportedJwtException e) {
            throw new UnauthorizedException(UserAuthErrorCode.TOKEN_INVALID);
        } catch (JwtException e) {
            throw new UnauthorizedException(UserAuthErrorCode.TOKEN_INVALID);
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

    /**
     * 解析 refresh token 签发时间（iat），用于密码重置后的失效判断。
     *
     * @param token JWT refresh token
     * @return 签发时间
     * @throws UnauthorizedException 当签名错误或 token 过期时
     */
    public Date getRefreshTokenIssuedAt(String token) {
        SecretKey key = Keys.hmacShaKeyFor(
                authProperties.getJwt().getRefreshSecret().getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getIssuedAt();
    }

    /**
     * 解析 refresh token 中的 rememberMe 标记。
     * 兼容旧 token（无该 claim），默认返回 false。
     */
    public boolean parseRememberMeFromRefreshToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(
                    authProperties.getJwt().getRefreshSecret().getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Object value = claims.get(REMEMBER_ME_CLAIM);
            return value != null && Boolean.parseBoolean(value.toString());
        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException(UserAuthErrorCode.TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new UnauthorizedException(UserAuthErrorCode.REFRESH_TOKEN_INVALID);
        }
    }
}
