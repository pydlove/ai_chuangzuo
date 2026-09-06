package com.aichuangzuo.admin.common.util;

/**
 * 头像 URL 兼容工具。
 *
 * <p>用户端上传头像返回 /api/v1/user/uploads/...，管理端上传头像返回 /api/v1/admin/uploads/...
 * 两边文件实际存在同一本地存储目录（data/uploads）下。管理端页面展示用户头像时，
 * 需要把用户端前缀改写成管理端前缀，否则在管理端域名下会裂图。
 */
public final class AvatarUrlUtil {

    private static final String USER_UPLOAD_PREFIX = "/api/v1/user/uploads/";
    private static final String ADMIN_UPLOAD_PREFIX = "/api/v1/admin/uploads/";

    private AvatarUrlUtil() {
    }

    /**
     * 把用户上传的头像 URL 转换为管理端可访问的 URL。
     *
     * @param avatarUrl 原始头像 URL
     * @return 转换后的 URL；如果为空或不是用户端上传路径，则原样返回
     */
    public static String normalizeForAdmin(String avatarUrl) {
        if (avatarUrl != null && avatarUrl.startsWith(USER_UPLOAD_PREFIX)) {
            return ADMIN_UPLOAD_PREFIX + avatarUrl.substring(USER_UPLOAD_PREFIX.length());
        }
        return avatarUrl;
    }
}
