package com.aichuangzuo.user.modules.auth.support;

import com.aichuangzuo.user.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 负责 refresh token 在 HttpOnly Cookie 中的写入与清除。
 *
 * <p>用途：给微信/手机浏览器做兜底存储。部分 WebView 会清理 localStorage，
 * Cookie 作为浏览器原生持久化机制更稳定；同时 HttpOnly 可防止 XSS 读取。</p>
 */
@Component
@RequiredArgsConstructor
public class AuthCookieHelper {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "aichuangzuo_refresh_token";

    private final AuthProperties authProperties;

    /**
     * 写入 refresh token Cookie。
     *
     * @param response   HTTP 响应
     * @param token      refresh token
     * @param maxAgeDays Cookie 最大存活天数
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String token, int maxAgeDays) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(isSecure())
                .sameSite(getSameSite())
                .path("/api/v1/user/auth")
                .maxAge(Duration.ofDays(maxAgeDays));
        response.addHeader("Set-Cookie", builder.build().toString());
    }

    /**
     * 清除 refresh token Cookie。
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(isSecure())
                .sameSite(getSameSite())
                .path("/api/v1/user/auth")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * 从请求中读取 refresh token Cookie。
     */
    public String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private boolean isSecure() {
        return authProperties.getCookie() != null && authProperties.getCookie().isSecure();
    }

    private String getSameSite() {
        if (authProperties.getCookie() == null) {
            return "Lax";
        }
        String value = authProperties.getCookie().getSameSite();
        if (value == null || value.isBlank()) {
            return "Lax";
        }
        return value;
    }
}
