package com.aichuangzuo.user.modules.audit.interceptor;

import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.audit.entity.UserAuditLog;
import com.aichuangzuo.user.modules.audit.service.UserAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class UserAuditLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "audit.startTime";
    private static final int MAX_PARAMS_LENGTH = 1024;

    private final UserAuditLogService userAuditLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long userId = SecurityUserContext.getCurrentUserId();
        if (userId == null) {
            return;
        }

        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        int durationMs = startTime == null ? 0 : (int) (System.currentTimeMillis() - startTime);

        String uri = request.getRequestURI();
        String method = request.getMethod();
        String module = resolveModule(uri);
        String actionType = resolveActionType(uri, method);

        UserAuditLog log = new UserAuditLog();
        log.setUserId(userId);
        log.setModule(module);
        log.setActionType(actionType);
        log.setRequestMethod(method);
        log.setRequestUri(uri);
        log.setRequestParams(truncate(request.getQueryString(), MAX_PARAMS_LENGTH));
        log.setClientIp(getClientIp(request));
        log.setUserAgent(truncate(request.getHeader("User-Agent"), 512));
        log.setStatusCode(response.getStatus());
        log.setErrorMsg(ex == null ? null : truncate(ex.getMessage(), 512));
        log.setDurationMs(durationMs);
        log.setCreatedAt(LocalDateTime.now());

        userAuditLogService.save(log);
    }

    private String resolveModule(String uri) {
        String path = uri.replaceFirst("/api/v1/user/", "");
        if (path.isEmpty()) {
            return "-";
        }
        int slash = path.indexOf('/');
        return slash > 0 ? path.substring(0, slash) : path;
    }

    private String resolveActionType(String uri, String method) {
        String path = uri.replaceFirst("/api/v1/user/", "");
        if (path.isEmpty()) {
            return method.toLowerCase();
        }
        int slash = path.indexOf('/');
        if (slash < 0) {
            return mapMethod(method);
        }
        String sub = path.substring(slash + 1);
        if (sub.isEmpty()) {
            return mapMethod(method);
        }
        // 若子路径是资源标识（如 bizNo/id），则根据 HTTP 方法映射
        if (sub.indexOf('/') < 0) {
            return mapMethod(method);
        }
        // 否则取最后一级作为 actionType，如 articles/{bizNo}/title-optimize
        int lastSlash = sub.lastIndexOf('/');
        return sub.substring(lastSlash + 1);
    }

    private String mapMethod(String method) {
        return switch (method.toUpperCase()) {
            case "GET" -> "query";
            case "POST" -> "create";
            case "PUT" -> "update";
            case "DELETE" -> "delete";
            case "PATCH" -> "patch";
            default -> method.toLowerCase();
        };
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
