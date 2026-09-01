package com.aichuangzuo.user.common.interceptor;

import com.aichuangzuo.shared.enums.error.SystemErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.cache.CacheUtil;
import com.aichuangzuo.user.modules.security.ratelimit.service.RateLimitConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final CacheUtil cacheUtil;
    private final RateLimitConfigService rateLimitConfigService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!rateLimitConfigService.isLoginRateLimitEnabled()) {
            return true;
        }

        String path = request.getRequestURI();
        String ip = getClientIp(request);
        String key = "user:rate-limit:" + path + ":" + ip;

        int maxRequests = getMaxRequests(path);
        int windowSeconds = getWindowSeconds(path);

        int count = cacheUtil.incrementAndGet(key, windowSeconds, TimeUnit.SECONDS);
        if (count > maxRequests) {
            throw new BusinessException(SystemErrorCode.RATE_LIMIT_ERROR);
        }
        return true;
    }

    private int getMaxRequests(String path) {
        if (path.contains("/email-codes")) return 5;
        if (path.contains("/login")) return 10;
        if (path.contains("/register")) return 5;
        if (path.contains("/qr-login")) return 20;
        return 100;
    }

    private int getWindowSeconds(String path) {
        if (path.contains("/register")) return 3600;
        return 60;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}