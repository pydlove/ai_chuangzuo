package com.aichuangzuo.user.modules.generation.service;

import com.aichuangzuo.shared.enums.error.UserGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenerationRateLimiterTest {

    private final GenerationRateLimiter limiter = new GenerationRateLimiter();

    @Test
    void check_shouldAllowRequestsUnderQuota() {
        assertDoesNotThrow(() -> limiter.check(1L, 3));
        assertDoesNotThrow(() -> limiter.check(1L, 3));
        assertDoesNotThrow(() -> limiter.check(1L, 3));
    }

    @Test
    void check_shouldBlockWhenExceedsQuota() {
        for (int i = 0; i < 3; i++) {
            limiter.check(2L, 3);
        }

        BusinessException ex = assertThrows(BusinessException.class,
                () -> limiter.check(2L, 3));
        assertEquals(UserGenerationErrorCode.GENERATION_RATE_LIMIT.getCode(), ex.getCode());
    }

    @Test
    void check_shouldIsolateDifferentUsers() {
        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> limiter.check(3L, 3));
        }
        assertThrows(BusinessException.class, () -> limiter.check(3L, 3));

        assertDoesNotThrow(() -> limiter.check(4L, 3));
    }
}
