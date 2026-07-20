package com.aichuangzuo.user.config;

import com.aichuangzuo.user.infrastructure.cache.CacheValue;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
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

    @Primary
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES));
        // 套餐权益缓存：10 分钟 TTL（权益配置极少变更）
        manager.registerCustomCache("planBenefits",
                Caffeine.newBuilder()
                        .maximumSize(100)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .build());
        // 公开定价目录：5 分钟 TTL（管理端改动后下次刷新即可生效）
        manager.registerCustomCache("planCatalog",
                Caffeine.newBuilder()
                        .maximumSize(20)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .build());
        // 套餐元数据：10 分钟 TTL（价格/邀请奖励读取）
        manager.registerCustomCache("plans",
                Caffeine.newBuilder()
                        .maximumSize(100)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .build());
        return manager;
    }
}
