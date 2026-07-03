package com.aichuangzuo.user.modules.auth.service.impl;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.exception.SystemException;
import com.aichuangzuo.user.config.AuthProperties;
import com.aichuangzuo.user.infrastructure.cache.CacheUtil;
import com.aichuangzuo.user.infrastructure.security.JwtUtil;
import com.aichuangzuo.user.modules.auth.converter.AuthConverter;
import com.aichuangzuo.user.modules.auth.dto.request.LoginRequest;
import com.aichuangzuo.user.modules.auth.dto.request.RefreshTokenRequest;
import com.aichuangzuo.user.modules.auth.dto.request.RegisterRequest;
import com.aichuangzuo.user.modules.auth.entity.IpRegisterLimit;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.entity.UserInviteRelation;
import com.aichuangzuo.user.modules.auth.entity.UserLoginLog;
import com.aichuangzuo.user.modules.auth.mapper.IpRegisterLimitMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserInviteRelationMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserLoginLogMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.auth.service.AuthService;
import com.aichuangzuo.user.modules.auth.service.CaptchaService;
import com.aichuangzuo.user.modules.auth.service.EmailCodeService;
import com.aichuangzuo.user.modules.auth.vo.AuthTokenVO;
import com.aichuangzuo.user.modules.auth.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserLoginLogMapper userLoginLogMapper;
    private final UserInviteRelationMapper userInviteRelationMapper;
    private final IpRegisterLimitMapper ipRegisterLimitMapper;
    private final EmailCodeService emailCodeService;
    private final CaptchaService captchaService;
    private final JwtUtil jwtUtil;
    private final CacheUtil cacheUtil;
    private final AuthConverter authConverter;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int MAX_LOGIN_FAIL = 5;
    private static final long LOGIN_FAIL_WINDOW_MINUTES = 5;
    private static final long ACCOUNT_LOCK_MINUTES = 30;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokenVO register(RegisterRequest request, String clientIp, String userAgent) {
        checkIpRegisterLimit(clientIp);

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(UserAuthErrorCode.PASSWORD_NOT_MATCH);
        }

        if (userMapper.selectByEmail(request.getEmail()) != null) {
            throw new BusinessException(UserAuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (!emailCodeService.validateEmailCode(request.getEmail(), request.getEmailCode())) {
            throw new BusinessException(UserAuthErrorCode.EMAIL_CODE_ERROR);
        }

        User user = new User();
        user.setBizNo("U" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setInviteCode(generateInviteCode());
        user.setUserStatus(1);
        user.setEmailVerified(1);
        userMapper.insert(user);

        if (request.getInviteCode() != null && !request.getInviteCode().isBlank()) {
            handleInviteRelation(user, request.getInviteCode().trim().toUpperCase());
        }

        ipRegisterLimitMapper.incrementRegisterCount(clientIp);
        IpRegisterLimit limit = ipRegisterLimitMapper.selectOne(
                new LambdaQueryWrapper<IpRegisterLimit>().eq(IpRegisterLimit::getClientIp, clientIp));
        if (limit != null && limit.getRegisterCount() >= authProperties.getRegister().getMaxPerIp()) {
            limit.setIsBlocked(1);
            ipRegisterLimitMapper.updateById(limit);
        }

        saveLoginLog(user.getId(), 2, clientIp, userAgent, 1, null);

        return buildAuthTokenVO(user);
    }

    private void checkIpRegisterLimit(String clientIp) {
        IpRegisterLimit limit = ipRegisterLimitMapper.selectOne(
                new LambdaQueryWrapper<IpRegisterLimit>().eq(IpRegisterLimit::getClientIp, clientIp));
        if (limit != null && limit.getIsBlocked() == 1) {
            throw new BusinessException(UserAuthErrorCode.OPERATION_TOO_FREQUENT);
        }
    }

    private void handleInviteRelation(User invitee, String inviteCode) {
        User inviter = userMapper.selectByInviteCode(inviteCode);
        if (inviter == null) {
            throw new BusinessException(UserAuthErrorCode.INVITE_CODE_INVALID);
        }
        UserInviteRelation relation = new UserInviteRelation();
        relation.setInviterId(inviter.getId());
        relation.setInviteeId(invitee.getId());
        relation.setInviteCode(inviteCode);
        relation.setSourceType(2);
        relation.setEffectiveStatus(0);
        userInviteRelationMapper.insert(relation);

        // 触发被邀请人 +5 创作币（钱包服务实现后替换为远程调用/事件）
        log.info("新用户 {} 通过邀请码 {} 注册，待发放 5 创作币", invitee.getEmail(), inviteCode);
    }

    private String generateInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            }
            String code = sb.toString();
            if (userMapper.selectByInviteCode(code) == null) {
                return code;
            }
        }
        throw new SystemException("生成邀请码失败");
    }

    private AuthTokenVO buildAuthTokenVO(User user) {
        AuthTokenVO vo = new AuthTokenVO();
        vo.setAccessToken(jwtUtil.generateAccessToken(user.getId()));
        vo.setRefreshToken(jwtUtil.generateRefreshToken(user.getId()));
        vo.setExpiresIn(Math.toIntExact(authProperties.getJwt().getAccessExpiration()));
        UserVO userVO = authConverter.toUserVO(user);
        vo.setUser(userVO);
        return vo;
    }

    private void saveLoginLog(Long userId, int loginType, String clientIp, String userAgent,
                              int status, String failReason) {
        UserLoginLog logRecord = new UserLoginLog();
        logRecord.setUserId(userId);
        logRecord.setLoginType(loginType);
        logRecord.setClientIp(clientIp);
        logRecord.setUserAgent(userAgent);
        logRecord.setLoginStatus(status);
        logRecord.setFailReason(failReason);
        logRecord.setCreatedAt(LocalDateTime.now());
        userLoginLogMapper.insert(logRecord);
    }

    @Override
    public AuthTokenVO login(LoginRequest request, String clientIp, String userAgent) {
        if (!captchaService.validateCaptcha(request.getCaptchaKey(), request.getCaptchaCode())) {
            throw new BusinessException(UserAuthErrorCode.CAPTCHA_ERROR);
        }

        String failKey = "user:auth:login-fail:" + request.getEmail();
        String lockKey = "user:auth:account-lock:" + request.getEmail();

        if (cacheUtil.get(lockKey) != null) {
            throw new BusinessException(UserAuthErrorCode.OPERATION_TOO_FREQUENT);
        }

        User user = userMapper.selectByEmail(request.getEmail());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            saveLoginLog(0L, 1, clientIp, userAgent, 0, "账号或密码错误");
            incrementLoginFail(failKey, lockKey);
            throw new BusinessException(UserAuthErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
        }

        if (user.getUserStatus() == 0) {
            throw new BusinessException(UserAuthErrorCode.ACCOUNT_DISABLED);
        }

        cacheUtil.delete(failKey);
        saveLoginLog(user.getId(), 1, clientIp, userAgent, 1, null);
        return buildAuthTokenVO(user);
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
    public AuthTokenVO refreshToken(RefreshTokenRequest request) {
        Long userId = jwtUtil.parseRefreshToken(request.getRefreshToken());
        User user = userMapper.selectById(userId);
        if (user == null || user.getUserStatus() == 0) {
            throw new BusinessException(UserAuthErrorCode.REFRESH_TOKEN_INVALID);
        }
        return buildAuthTokenVO(user);
    }

    @Override
    public void logout(String accessToken) {
        String jti = jwtUtil.getJti(accessToken);
        long ttlMillis = jwtUtil.getExpiration(accessToken).getTime() - System.currentTimeMillis();
        if (ttlMillis > 0) {
            cacheUtil.set("user:auth:token-blacklist:" + jti, true, ttlMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }
}
