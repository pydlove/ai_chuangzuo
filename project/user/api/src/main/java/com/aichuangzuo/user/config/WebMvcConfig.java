package com.aichuangzuo.user.config;

import com.aichuangzuo.user.common.interceptor.RateLimitInterceptor;
import com.aichuangzuo.user.modules.audit.interceptor.UserAuditLogInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final UserAuditLogInterceptor userAuditLogInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/user/auth/**");

        registry.addInterceptor(userAuditLogInterceptor)
                .addPathPatterns("/api/v1/user/**")
                .excludePathPatterns(
                        "/api/v1/user/auth/**",
                        "/api/v1/user/internal/**",
                        "/api/v1/user/learn/**",
                        "/api/v1/user/home/**",
                        "/api/v1/user/plans",
                        "/api/v1/user/plans/**",
                        "/api/v1/user/export-templates",
                        "/api/v1/user/export-templates/**"
                );
    }
}