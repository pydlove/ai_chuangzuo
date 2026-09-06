package com.aichuangzuo.user.modules.activity.interceptor;

import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.activity.service.UserActivityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 活跃打点拦截器：认证后的每个用户请求都会刷新活跃时间（service 内部 60s 节流落库）。
 * 注册在 AccessControlInterceptor 之后，SecurityUserContext 已被 JwtAuthenticationFilter 填充。
 */
@Component
@RequiredArgsConstructor
public class UserActivityInterceptor implements HandlerInterceptor {

    private final UserActivityService userActivityService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        userActivityService.track(SecurityUserContext.getCurrentUserId());
        return true;
    }
}
