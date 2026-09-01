package com.aichuangzuo.user.config;

import com.aichuangzuo.user.infrastructure.security.InternalKeyAuthenticationFilter;
import com.aichuangzuo.user.infrastructure.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final InternalKeyAuthenticationFilter internalKeyAuthenticationFilter;
    private final Environment environment;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/api/v1/user/auth/qr-login/scan").authenticated();
                auth.requestMatchers("/api/v1/user/auth/**").permitAll();
                auth.requestMatchers("/api/v1/user/learn/**").permitAll();
                auth.requestMatchers("/api/v1/user/plans").permitAll();
                auth.requestMatchers("/api/v1/user/plans/newcomer-offer").authenticated();
                auth.requestMatchers("/api/v1/user/export-templates").permitAll();
                auth.requestMatchers("/api/v1/user/home/**").permitAll();
                auth.requestMatchers("/api/v1/public/**").permitAll();
                auth.requestMatchers(HttpMethod.GET, "/api/v1/user/lottery/campaigns/current").permitAll();
                auth.requestMatchers(HttpMethod.GET, "/api/v1/user/lottery/display-winners").permitAll();
                auth.requestMatchers(HttpMethod.GET, "/api/v1/user/share-config/**").permitAll();
                auth.requestMatchers("/api/v1/public/payment/xunhupay/notify").permitAll();
                auth.requestMatchers("/api/v1/user/payment/config").permitAll();
                if (environment.matchesProfiles("test")) {
                    auth.requestMatchers("/__test/**").permitAll();
                }
                auth.requestMatchers("/uploads/**").permitAll();
                auth.requestMatchers("/api/v1/user/uploads/**").permitAll();
                auth.requestMatchers("/doc.html", "/webjars/**", "/swagger-resources/**", "/v3/api-docs/**").permitAll();
                auth.anyRequest().authenticated();
            })
            .addFilterBefore(internalKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
