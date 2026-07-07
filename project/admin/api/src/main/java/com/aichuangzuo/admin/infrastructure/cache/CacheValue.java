package com.aichuangzuo.admin.infrastructure.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheValue<T> {
    private T value;
    private long expireAtMillis;
}
