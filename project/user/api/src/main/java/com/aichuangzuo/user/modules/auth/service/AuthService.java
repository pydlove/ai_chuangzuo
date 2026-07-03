package com.aichuangzuo.user.modules.auth.service;

import com.aichuangzuo.user.modules.auth.dto.request.LoginRequest;
import com.aichuangzuo.user.modules.auth.dto.request.RefreshTokenRequest;
import com.aichuangzuo.user.modules.auth.dto.request.RegisterRequest;
import com.aichuangzuo.user.modules.auth.vo.AuthTokenVO;

public interface AuthService {
    AuthTokenVO register(RegisterRequest request, String clientIp, String userAgent);
    AuthTokenVO login(LoginRequest request, String clientIp, String userAgent);
    AuthTokenVO refreshToken(RefreshTokenRequest request);
    void logout(String accessToken);
}
