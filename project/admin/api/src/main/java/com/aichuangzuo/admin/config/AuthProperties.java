package com.aichuangzuo.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {
    private Jwt jwt;

    @Data
    public static class Jwt {
        private String accessSecret;
        private String refreshSecret;
        private Long accessExpiration;
        private Long refreshExpiration;
    }
}
