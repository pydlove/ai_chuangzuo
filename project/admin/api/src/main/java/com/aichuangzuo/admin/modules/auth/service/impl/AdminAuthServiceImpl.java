package com.aichuangzuo.admin.modules.auth.service.impl;

import com.aichuangzuo.admin.config.AuthProperties;
import com.aichuangzuo.admin.infrastructure.cache.CacheUtil;
import com.aichuangzuo.admin.infrastructure.security.JwtUtil;
import com.aichuangzuo.admin.modules.auth.converter.AdminAuthConverter;
import com.aichuangzuo.admin.modules.auth.dto.request.AdminLoginRequest;
import com.aichuangzuo.admin.modules.auth.dto.request.AdminRefreshTokenRequest;
import com.aichuangzuo.admin.modules.auth.entity.AdminLoginLog;
import com.aichuangzuo.admin.modules.auth.entity.AdminUser;
import com.aichuangzuo.admin.modules.auth.mapper.AdminLoginLogMapper;
import com.aichuangzuo.admin.modules.auth.mapper.AdminUserMapper;
import com.aichuangzuo.admin.modules.auth.service.AdminAuthService;
import com.aichuangzuo.admin.modules.auth.vo.AdminAuthTokenVO;
import com.aichuangzuo.admin.modules.auth.vo.AdminUserVO;
import com.aichuangzuo.shared.enums.error.AdminAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final AdminLoginLogMapper adminLoginLogMapper;
    private final JwtUtil jwtUtil;
    private final CacheUtil cacheUtil;
    private final AdminAuthConverter adminAuthConverter;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    private static final int MAX_LOGIN_FAIL = 5;
    private static final long LOGIN_FAIL_WINDOW_MINUTES = 5;
    private static final long ACCOUNT_LOCK_MINUTES = 30;

    private static final String LOGIN_FAIL_PREFIX = "admin:auth:login-fail:";
    private static final String ACCOUNT_LOCK_PREFIX = "admin:auth:account-lock:";
    private static final String TOKEN_BLACKLIST_PREFIX = "admin:auth:token-blacklist:";

    @Override
    public AdminAuthTokenVO login(AdminLoginRequest request, String clientIp, String userAgent) {
        String failKey = LOGIN_FAIL_PREFIX + request.getUsername();
        String lockKey = ACCOUNT_LOCK_PREFIX + request.getUsername();

        if (cacheUtil.get(lockKey) != null) {
            saveLoginLog(0L, 1, clientIp, userAgent, 0, "账号登录锁定中");
            throw new BusinessException(AdminAuthErrorCode.OPERATION_TOO_FREQUENT);
        }

        AdminUser adminUser = adminUserMapper.selectByUsername(request.getUsername());
        if (adminUser == null || !passwordEncoder.matches(request.getPassword(), adminUser.getPasswordHash())) {
            saveLoginLog(0L, 1, clientIp, userAgent, 0, "账号或密码错误");
            incrementLoginFail(failKey, lockKey);
            throw new BusinessException(AdminAuthErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
        }

        if (adminUser.getStatus() == 0) {
            saveLoginLog(adminUser.getId(), 1, clientIp, userAgent, 0, "账号已被禁用");
            throw new BusinessException(AdminAuthErrorCode.ACCOUNT_DISABLED);
        }

        cacheUtil.delete(failKey);
        adminUser.setLastLoginAt(LocalDateTime.now());
        adminUserMapper.updateById(adminUser);
        saveLoginLog(adminUser.getId(), 1, clientIp, userAgent, 1, null);

        return buildAuthTokenVO(adminUser);
    }

    private void incrementLoginFail(String failKey, String lockKey) {
        Integer count = cacheUtil.get(failKey);
        if (count == null) {
            count = 0;
        }
        count++;
        cacheUtil.set(failKey, count, LOGIN_FAIL_WINDOW_MINUTES, TimeUnit.MINUTES);
        if (count >= MAX_LOGIN_FAIL) {
            cacheUtil.set(lockKey, true, ACCOUNT_LOCK_MINUTES, TimeUnit.MINUTES);
            cacheUtil.delete(failKey);
        }
    }

    @Override
    public AdminAuthTokenVO refreshToken(AdminRefreshTokenRequest request) {
        Long adminUserId = jwtUtil.parseRefreshToken(request.getRefreshToken());
        AdminUser adminUser = adminUserMapper.selectById(adminUserId);
        if (adminUser == null || adminUser.getStatus() == 0) {
            throw new BusinessException(AdminAuthErrorCode.REFRESH_TOKEN_INVALID);
        }
        return buildAuthTokenVO(adminUser);
    }

    @Override
    public void logout(String accessToken) {
        String jti = jwtUtil.getJti(accessToken);
        long ttlMillis = jwtUtil.getExpiration(accessToken).getTime() - System.currentTimeMillis();
        if (ttlMillis > 0) {
            cacheUtil.set(TOKEN_BLACKLIST_PREFIX + jti, true, ttlMillis, TimeUnit.MILLISECONDS);
        }
    }

    private AdminAuthTokenVO buildAuthTokenVO(AdminUser adminUser) {
        AdminAuthTokenVO vo = new AdminAuthTokenVO();
        vo.setAccessToken(jwtUtil.generateAccessToken(adminUser.getId()));
        vo.setRefreshToken(jwtUtil.generateRefreshToken(adminUser.getId()));
        vo.setExpiresIn(Math.toIntExact(authProperties.getJwt().getAccessExpiration()));
        AdminUserVO adminUserVO = adminAuthConverter.toAdminUserVO(adminUser);
        vo.setUser(adminUserVO);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveLoginLog(Long adminUserId, int loginType, String clientIp, String userAgent,
                              int status, String failReason) {
        AdminLoginLog logRecord = new AdminLoginLog();
        logRecord.setAdminUserId(adminUserId);
        logRecord.setLoginType(loginType);
        logRecord.setClientIp(clientIp);
        logRecord.setUserAgent(userAgent);
        logRecord.setLoginStatus(status);
        logRecord.setFailReason(failReason);
        logRecord.setCreatedAt(LocalDateTime.now());
        adminLoginLogMapper.insert(logRecord);
    }
}
