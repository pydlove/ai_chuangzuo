package com.aichuangzuo.admin.config;

import com.aichuangzuo.admin.infrastructure.cache.CacheValue;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CaffeineConfig {

    @Bean("authCache")
    public com.github.benmanes.caffeine.cache.Cache<String, CacheValue<?>> authCache() {
        return Caffeine.newBuilder()
                .maximumSize(100000)
                .expireAfter(new Expiry<String, CacheValue<?>>() {
                    @Override
                    public long expireAfterCreate(String key, CacheValue<?> value, long currentTime) {
                        long ttlMillis = value.getExpireAtMillis() - System.currentTimeMillis();
                        long ttlNanos = TimeUnit.MILLISECONDS.toNanos(ttlMillis);
                        return Math.max(ttlNanos, TimeUnit.SECONDS.toNanos(1));
                    }

                    @Override
                    public long expireAfterUpdate(String key, CacheValue<?> value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(String key, CacheValue<?> value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }
}
