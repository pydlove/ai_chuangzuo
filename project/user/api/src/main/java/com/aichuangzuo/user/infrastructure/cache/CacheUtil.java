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
        return value == null ? null : value.getValue();
    }

    /**
     * 对缓存中的整型计数器原子地 +1，并返回增加后的值。
     *
     * <p>若 key 不存在或已过期，则初始化为 1；否则在原有值基础上加 1。
     * 使用 {@link com.github.benmanes.caffeine.cache.Cache#asMap()} 的
     * {@code compute} 保证并发安全，避免 "get-then-set" 竞态。
     *
     * @param key      缓存 key
     * @param duration 首次创建时的过期时长
     * @param unit     时长单位
     * @return 增加后的计数值
     */
    @SuppressWarnings("unchecked")
    public int incrementAndGet(String key, long duration, TimeUnit unit) {
        long now = System.currentTimeMillis();
        long expireAt = now + unit.toMillis(duration);
        CacheValue<Integer> result = (CacheValue<Integer>) authCache.asMap().compute(key, (k, old) -> {
            if (old == null || old.getValue() == null || old.getExpireAtMillis() <= now) {
                return new CacheValue<>(1, expireAt);
            }
            return new CacheValue<>((Integer) old.getValue() + 1, old.getExpireAtMillis());
        });
        return result.getValue();
    }

    public <T> void set(String key, T value, long duration, TimeUnit unit) {
        long expireAt = System.currentTimeMillis() + unit.toMillis(duration);
        authCache.put(key, new CacheValue<>(value, expireAt));
    }

    public void delete(String key) {
        authCache.invalidate(key);
    }
}
