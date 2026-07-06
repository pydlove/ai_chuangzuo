package com.aichuangzuo.admin.infrastructure.security;

public final class SecurityAdminContext {
    private static final ThreadLocal<Long> CURRENT_ADMIN_USER = new ThreadLocal<>();

    private SecurityAdminContext() {
    }

    public static void setCurrentAdminUserId(Long adminUserId) {
        CURRENT_ADMIN_USER.set(adminUserId);
    }

    public static Long getCurrentAdminUserId() {
        return CURRENT_ADMIN_USER.get();
    }

    public static void clear() {
        CURRENT_ADMIN_USER.remove();
    }
}
