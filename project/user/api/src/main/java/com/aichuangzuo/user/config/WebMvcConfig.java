package com.aichuangzuo.user.config;

import com.aichuangzuo.user.common.interceptor.AccessControlInterceptor;
import com.aichuangzuo.user.common.interceptor.RateLimitInterceptor;
import com.aichuangzuo.user.modules.audit.interceptor.UserAuditLogInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Paths;
import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${storage.local.base-path:data/uploads}")
    private String storageBasePath;

    private final AccessControlInterceptor accessControlInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final UserAuditLogInterceptor userAuditLogInterceptor;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessControlInterceptor)
                .addPathPatterns("/api/v1/user/**")
                .excludePathPatterns("/api/v1/user/internal/**");

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

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + Paths.get(storageBasePath).toAbsolutePath() + "/");
    }
}
