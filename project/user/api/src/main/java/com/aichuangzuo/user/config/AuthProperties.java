package com.aichuangzuo.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {
    private Jwt jwt;
    private Register register;

    @Data
    public static class Jwt {
        private String accessSecret;
        private String refreshSecret;
        private Long accessExpiration;
        private Long refreshExpiration;
    }

    @Data
    public static class Register {
        private Integer maxPerIp;
    }
}
