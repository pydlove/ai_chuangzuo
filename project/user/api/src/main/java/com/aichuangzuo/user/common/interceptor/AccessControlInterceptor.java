package com.aichuangzuo.user.common.interceptor;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.security.accesscontrol.service.AccessControlService;
import com.aichuangzuo.user.modules.security.accesscontrol.vo.AccessControlSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessControlInterceptor implements HandlerInterceptor {

    private final AccessControlService accessControlService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String ip = getClientIp(request);
        AccessControlSnapshot rules = accessControlService.loadActiveRules();

        if (!rules.isIpAllowed(ip)) {
            log.warn("访问控制拦截 IP={} path={}", ip, request.getRequestURI());
            throw new BusinessException(UserAuthErrorCode.ACCESS_DENIED);
        }

        Long userId = SecurityUserContext.getCurrentUserId();
        if (userId != null) {
            User user = accessControlService.getUserById(userId);
            if (user != null) {
                String idStr = String.valueOf(userId);
                if (!rules.isAccountAllowed(idStr)) {
                    log.warn("访问控制拦截账号 userId={} path={}", userId, request.getRequestURI());
                    throw new BusinessException(UserAuthErrorCode.ACCESS_DENIED);
                }
                if (user.getEmail() != null && !rules.isAccountAllowed(user.getEmail())) {
                    log.warn("访问控制拦截账号 email={} userId={} path={}", user.getEmail(), userId, request.getRequestURI());
                    throw new BusinessException(UserAuthErrorCode.ACCESS_DENIED);
                }
            }
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}
