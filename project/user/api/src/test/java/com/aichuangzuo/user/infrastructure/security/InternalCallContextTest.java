package com.aichuangzuo.user.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalCallContextTest {

    @Test
    void markThenClear_followsLifecycle() {
        assertFalse(InternalCallContext.isInternal());
        InternalCallContext.markInternal();
        assertTrue(InternalCallContext.isInternal());
        InternalCallContext.clear();
        assertFalse(InternalCallContext.isInternal());
    }

    @Test
    void clearWithoutMark_isSafe() {
        InternalCallContext.clear();
        assertFalse(InternalCallContext.isInternal());
    }
}
