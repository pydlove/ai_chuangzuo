package com.aichuangzuo.admin.modules.auth.service;

import com.aichuangzuo.admin.modules.auth.dto.request.AdminLoginRequest;
import com.aichuangzuo.admin.modules.auth.dto.request.AdminRefreshTokenRequest;
import com.aichuangzuo.admin.modules.auth.vo.AdminAuthTokenVO;

public interface AdminAuthService {

    AdminAuthTokenVO login(AdminLoginRequest request, String clientIp, String userAgent);

    AdminAuthTokenVO refreshToken(AdminRefreshTokenRequest request);

    void logout(String accessToken);
}
