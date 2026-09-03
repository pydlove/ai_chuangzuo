package com.aichuangzuo.user.modules.auth.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.auth.dto.request.LoginRequest;
import com.aichuangzuo.user.modules.auth.dto.request.RefreshTokenRequest;
import com.aichuangzuo.user.modules.auth.dto.request.RegisterRequest;
import com.aichuangzuo.user.modules.auth.dto.request.ResetPasswordRequest;
import com.aichuangzuo.user.modules.auth.dto.request.SendEmailCodeRequest;
import com.aichuangzuo.user.modules.auth.dto.request.SendSmsCodeRequest;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.auth.service.AuthService;
import com.aichuangzuo.user.modules.auth.service.EmailCodeService;
import com.aichuangzuo.user.modules.auth.service.SmsCodeService;
import com.aichuangzuo.user.config.AuthProperties;
import com.aichuangzuo.user.modules.auth.support.AuthCookieHelper;
import com.aichuangzuo.user.modules.auth.vo.AuthTokenVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户认证")
@RestController
@RequestMapping("/api/v1/user/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final EmailCodeService emailCodeService;
    private final AuthService authService;
    private final SmsCodeService smsCodeService;
    private final AuthCookieHelper authCookieHelper;
    private final AuthProperties authProperties;

    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/email-codes")
    public Result<Void> sendEmailCode(@Valid @RequestBody SendEmailCodeRequest request) {
        log.info("发送邮箱验证码, userId={}, email={}", SecurityUserContext.getCurrentUserId(), request.getEmail());
        emailCodeService.sendEmailCode(request.getEmail());
        return Result.success();
    }
    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms-codes")
    public Result<Void> sendSmsCode(@Valid @RequestBody SendSmsCodeRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        log.info("发送短信验证码, userId={}, phone={}", SecurityUserContext.getCurrentUserId(), request.getPhone());
        smsCodeService.sendSmsCode(request.getPhone(), clientIp);
        return Result.success();
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<AuthTokenVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
                                     HttpServletResponse response) {
        String clientIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        log.info("用户登录, userId={}, email={}, clientIp={}, userAgent={}", SecurityUserContext.getCurrentUserId(), request.getEmail(), clientIp, userAgent);
        AuthTokenVO vo = authService.login(request, clientIp, userAgent);
        setRefreshTokenCookie(response, vo);
        return Result.success(vo);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<AuthTokenVO> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest,
                                        HttpServletResponse response) {
        String clientIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        log.info("用户注册, userId={}, email={}, inviteCode={}, clientIp={}", SecurityUserContext.getCurrentUserId(), request.getEmail(), request.getInviteCode(), clientIp);
        AuthTokenVO vo = authService.register(request, clientIp, userAgent);
        setRefreshTokenCookie(response, vo);
        return Result.success(vo);
    }

    @Operation(summary = "重置密码")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                      HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        log.info("重置密码, userId={}, email={}, clientIp={}", SecurityUserContext.getCurrentUserId(), request.getEmail(), clientIp);
        authService.resetPassword(request, clientIp);
        return Result.success();
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh-token")
    public Result<AuthTokenVO> refreshToken(@RequestBody RefreshTokenRequest request,
                                            HttpServletRequest httpRequest,
                                            HttpServletResponse response) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = authCookieHelper.extractRefreshTokenFromCookie(httpRequest);
        }
        log.info("刷新Token, userId={}, refreshTokenPresent={}, refreshTokenFromCookie={}",
                SecurityUserContext.getCurrentUserId(),
                refreshToken != null,
                authCookieHelper.extractRefreshTokenFromCookie(httpRequest) != null);
        RefreshTokenRequest actualRequest = new RefreshTokenRequest();
        actualRequest.setRefreshToken(refreshToken);
        AuthTokenVO vo = authService.refreshToken(actualRequest);
        setRefreshTokenCookie(response, vo);
        return Result.success(vo);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                               HttpServletResponse response) {
        log.info("退出登录, userId={}, authorizationPresent={}", SecurityUserContext.getCurrentUserId(), authorization != null);
        authCookieHelper.clearRefreshTokenCookie(response);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            authService.logout(authorization.substring(7));
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

    private void setRefreshTokenCookie(HttpServletResponse response, AuthTokenVO vo) {
        if (vo == null || vo.getRefreshToken() == null) {
            return;
        }
        int maxAgeDays = (vo.getRememberMe() != null && vo.getRememberMe())
                ? (int) (authProperties.getJwt().getRememberMeRefreshExpiration() / 86400)
                : (int) (authProperties.getJwt().getRefreshExpiration() / 86400);
        authCookieHelper.setRefreshTokenCookie(response, vo.getRefreshToken(), maxAgeDays);
    }
}
