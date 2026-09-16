package com.aichuangzuo.user.infrastructure.security;

/**
 * 内部调用上下文：标记当前请求携带了合法的 X-Internal-Key。
 *
 * <p>用于请求级绿通（如模拟运营的测试支付放行），由 InternalKeyAuthenticationFilter
 * 在请求进入业务逻辑前打标、请求结束后清除；不依赖全局配置开关。
 */
public final class InternalCallContext {

    private static final ThreadLocal<Boolean> INTERNAL = new ThreadLocal<>();

    private InternalCallContext() {
    }

    public static void markInternal() {
        INTERNAL.set(Boolean.TRUE);
    }

    public static boolean isInternal() {
        return Boolean.TRUE.equals(INTERNAL.get());
    }

    public static void clear() {
        INTERNAL.remove();
    }
}
