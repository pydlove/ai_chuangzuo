package com.aichuangzuo.admin.modules.auth.controller;

import com.aichuangzuo.admin.modules.auth.dto.request.AdminLoginRequest;
import com.aichuangzuo.admin.modules.auth.dto.request.AdminRefreshTokenRequest;
import com.aichuangzuo.admin.modules.auth.service.AdminAuthService;
import com.aichuangzuo.admin.modules.auth.vo.AdminAuthTokenVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端认证")
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<AdminAuthTokenVO> login(@Valid @RequestBody AdminLoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return Result.success(adminAuthService.login(request, clientIp, userAgent));
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh-token")
    public Result<AdminAuthTokenVO> refreshToken(@Valid @RequestBody AdminRefreshTokenRequest request) {
        return Result.success(adminAuthService.refreshToken(request));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            adminAuthService.logout(authorization.substring(7));
        }
        return Result.success();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}
