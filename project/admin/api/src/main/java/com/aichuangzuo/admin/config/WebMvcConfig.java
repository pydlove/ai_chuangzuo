package com.aichuangzuo.admin.config;

import com.aichuangzuo.admin.common.interceptor.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${storage.local.base-path:data/uploads}")
    private String storageBasePath;

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/admin/auth/login");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absoluteBase = "file:" + Paths.get(storageBasePath).toAbsolutePath() + "/";
        // 旧路径：兼容已上传文件
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absoluteBase);
        // 新路径：走 /api/v1/admin 代理，避免线上 /uploads 未代理导致头像裂图
        registry.addResourceHandler("/api/v1/admin/uploads/**")
                .addResourceLocations(absoluteBase);
    }
}
