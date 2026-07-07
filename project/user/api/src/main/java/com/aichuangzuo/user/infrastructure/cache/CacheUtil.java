package com.aichuangzuo.user.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class CacheUtil {

    private final Cache<String, CacheValue<?>> authCache;

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        CacheValue<T> value = (CacheValue<T>) authCache.getIfPresent(key);
        if (value == null) {
            return null;
        }
        if (System.currentTimeMillis() > value.getExpireAtMillis()) {
            authCache.invalidate(key);
            return null;
        }
        return value.getValue();
    }

    public <T> void set(String key, T value, long duration, TimeUnit unit) {
        long expireAt = System.currentTimeMillis() + unit.toMillis(duration);
        authCache.put(key, new CacheValue<>(value, expireAt));
    }

    public void delete(String key) {
        authCache.invalidate(key);
    }
}
