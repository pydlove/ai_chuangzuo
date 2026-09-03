package com.aichuangzuo.user.modules.auth.dto.request;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    /**
     * refresh token。前端正常会从请求体携带；当浏览器 localStorage 被清理时，
     * 可由 HttpOnly Cookie 兜底，此时该字段可为空。
     */
    private String refreshToken;
}
