package com.aichuangzuo.user.modules.auth.service.impl;

import com.aichuangzuo.shared.enums.BlockStatusEnum;
import com.aichuangzuo.shared.enums.UserStatusEnum;
import com.aichuangzuo.shared.enums.VerifyStatusEnum;
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
import com.aichuangzuo.user.modules.auth.dto.request.ResetPasswordRequest;
import com.aichuangzuo.user.modules.auth.entity.IpRegisterLimit;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.entity.UserLoginLog;
import com.aichuangzuo.user.modules.auth.mapper.IpRegisterLimitMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserLoginLogMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.user.service.InviteRewardService;
import com.aichuangzuo.user.modules.auth.service.AuthService;
import com.aichuangzuo.user.modules.auth.service.EmailCodeService;
import com.aichuangzuo.user.modules.auth.service.SmsCodeService;
import com.aichuangzuo.user.modules.experience.service.ExperienceTokenService;
import com.aichuangzuo.user.modules.auth.vo.AuthTokenVO;
import com.aichuangzuo.user.modules.auth.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserLoginLogMapper userLoginLogMapper;
    private final InviteRewardService inviteRewardService;
    private final ExperienceTokenService experienceTokenService;
    private final IpRegisterLimitMapper ipRegisterLimitMapper;
    private final EmailCodeService emailCodeService;
    private final JwtUtil jwtUtil;
    private final SmsCodeService smsCodeService;
    private final CacheUtil cacheUtil;
    private final AuthConverter authConverter;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int MAX_LOGIN_FAIL = 5;
    private static final long LOGIN_FAIL_WINDOW_MINUTES = 5;
    private static final long ACCOUNT_LOCK_MINUTES = 30;
    private static final String PASSWORD_RESET_AT_PREFIX = "user:auth:password-reset-at:";

   @Override
   @Transactional(rollbackFor = Exception.class)
   public void resetPassword(ResetPasswordRequest request, String clientIp) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(UserAuthErrorCode.PASSWORD_NOT_MATCH);
        }

        boolean isEmail = StringUtils.hasText(request.getEmail());
        boolean isPhone = StringUtils.hasText(request.getPhone());
        if (!isEmail && !isPhone) {
            throw new BusinessException(UserAuthErrorCode.PHONE_OR_EMAIL_REQUIRED);
        }
        if (isEmail && isPhone) {
            throw new BusinessException(UserAuthErrorCode.PHONE_OR_EMAIL_REQUIRED);
        }

        User user;
        if (isEmail) {
            String email = request.getEmail().trim().toLowerCase();
            user = userMapper.selectByEmail(email);
            if (user == null) {
                throw new BusinessException(UserAuthErrorCode.RESET_PASSWORD_FAILED);
            }
            if (!emailCodeService.validateEmailCode(email, request.getEmailCode())) {
                throw new BusinessException(UserAuthErrorCode.EMAIL_CODE_ERROR);
            }
        } else {
            String phone = normalizePhone(request.getPhone());
            user = userMapper.selectByPhone(phone);
            if (user == null) {
                throw new BusinessException(UserAuthErrorCode.PHONE_NOT_FOUND);
            }
            if (!smsCodeService.validateSmsCode(phone, request.getSmsCode())) {
                throw new BusinessException(UserAuthErrorCode.SMS_CODE_ERROR);
            }
        }

        String newHash = passwordEncoder.encode(request.getPassword());
        userMapper.updatePassword(user.getId(), newHash);

        long refreshTtlSeconds = authProperties.getJwt().getRefreshExpiration();
        // 按秒归一化时间戳，与 JWT iat（秒级精度）对齐，避免同秒内 iat 误判
        long nowSec = System.currentTimeMillis() / 1000;
        cacheUtil.set(PASSWORD_RESET_AT_PREFIX + user.getId(),
                new Date(nowSec * 1000),
                refreshTtlSeconds,
                TimeUnit.SECONDS);

        saveLoginLog(user.getId(), 3, clientIp, "reset-password", 1, null);
   }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokenVO register(RegisterRequest request, String clientIp, String userAgent) {
        String inviteCode = request.getInviteCode() == null ? null
                : request.getInviteCode().trim().toUpperCase();
        String experienceToken = request.getExperienceToken() == null ? null
                : request.getExperienceToken().trim();

        if (StringUtils.hasText(inviteCode) && StringUtils.hasText(experienceToken)) {
            throw new BusinessException(UserAuthErrorCode.REGISTER_PARAM_INVALID);
        }

        checkIpRegisterLimit(clientIp);

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(UserAuthErrorCode.PASSWORD_NOT_MATCH);
        }

        boolean isEmail = StringUtils.hasText(request.getEmail());
        boolean isPhone = StringUtils.hasText(request.getPhone());
        if (!isEmail && !isPhone) {
            throw new BusinessException(UserAuthErrorCode.PHONE_OR_EMAIL_REQUIRED);
        }
        if (isEmail && isPhone) {
            throw new BusinessException(UserAuthErrorCode.PHONE_OR_EMAIL_REQUIRED);
        }

        String email = null;
        String phone = null;
        if (isEmail) {
            email = request.getEmail().trim().toLowerCase();
            if (userMapper.selectByEmail(email) != null) {
                throw new BusinessException(UserAuthErrorCode.EMAIL_ALREADY_EXISTS);
            }
            if (!emailCodeService.validateEmailCode(request.getEmail(), request.getEmailCode())) {
                throw new BusinessException(UserAuthErrorCode.EMAIL_CODE_ERROR);
            }
        } else {
            phone = normalizePhone(request.getPhone());
            if (userMapper.selectByPhone(phone) != null) {
                throw new BusinessException(UserAuthErrorCode.PHONE_ALREADY_EXISTS);
            }
            if (!smsCodeService.validateSmsCode(phone, request.getSmsCode())) {
                throw new BusinessException(UserAuthErrorCode.SMS_CODE_ERROR);
            }
        }

        if (inviteCode != null && !inviteCode.isBlank()) {
            if (userMapper.selectByInviteCode(inviteCode) == null) {
                throw new BusinessException(UserAuthErrorCode.INVITE_CODE_INVALID);
            }
        }

        User user = new User();
        user.setBizNo("U" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setInviteCode(generateInviteCode());
        user.setUserStatus(UserStatusEnum.ENABLED.getCode());
        user.setEmailVerified(isEmail ? VerifyStatusEnum.VERIFIED.getCode() : VerifyStatusEnum.UNVERIFIED.getCode());
        user.setPhoneVerified(isPhone ? VerifyStatusEnum.VERIFIED.getCode() : VerifyStatusEnum.UNVERIFIED.getCode());
        user.setNickname("用户" + user.getInviteCode());

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            if (isEmail) {
                throw new BusinessException(UserAuthErrorCode.EMAIL_ALREADY_EXISTS);
            } else {
                throw new BusinessException(UserAuthErrorCode.PHONE_ALREADY_EXISTS);
            }
        }

        if (inviteCode != null && !inviteCode.isBlank()) {
            handleInviteRelation(user, inviteCode);
        } else if (experienceToken != null && !experienceToken.isBlank()) {
            experienceTokenService.consume(user.getId(), experienceToken);
        }

        ipRegisterLimitMapper.incrementRegisterCount(clientIp);
        IpRegisterLimit limit = ipRegisterLimitMapper.selectOne(
                new LambdaQueryWrapper<IpRegisterLimit>().eq(IpRegisterLimit::getClientIp, clientIp));
        if (limit != null && limit.getRegisterCount() >= authProperties.getRegister().getMaxPerIp()) {
            limit.setIsBlocked(BlockStatusEnum.BLOCKED.getCode());
            ipRegisterLimitMapper.updateById(limit);
        }

        saveLoginLog(user.getId(), 2, clientIp, userAgent, 1, null);

        boolean rememberMe = request.getRememberMe() != null && request.getRememberMe();
        return buildAuthTokenVO(user, rememberMe);
}

    private void checkIpRegisterLimit(String clientIp) {
        IpRegisterLimit limit = ipRegisterLimitMapper.selectOne(
                new LambdaQueryWrapper<IpRegisterLimit>().eq(IpRegisterLimit::getClientIp, clientIp));
        if (limit != null && limit.getIsBlocked() == BlockStatusEnum.BLOCKED.getCode()) {
            throw new BusinessException(UserAuthErrorCode.OPERATION_TOO_FREQUENT);
        }
    }

    private void handleInviteRelation(User invitee, String inviteCode) {
        inviteRewardService.rewardAfterRegister(invitee, inviteCode);
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

    private AuthTokenVO buildAuthTokenVO(User user, boolean rememberMe) {
        AuthTokenVO vo = new AuthTokenVO();
        vo.setAccessToken(jwtUtil.generateAccessToken(user.getId()));
        vo.setRefreshToken(jwtUtil.generateRefreshToken(user.getId(), rememberMe));
        vo.setExpiresIn(Math.toIntExact(authProperties.getJwt().getAccessExpiration()));
        vo.setRememberMe(rememberMe);
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
        boolean isEmail = StringUtils.hasText(request.getEmail());
        boolean isPhone = StringUtils.hasText(request.getPhone());
        if (!isEmail && !isPhone) {
            throw new BusinessException(UserAuthErrorCode.PHONE_OR_EMAIL_REQUIRED);
        }
        if (isEmail && isPhone) {
            throw new BusinessException(UserAuthErrorCode.PHONE_OR_EMAIL_REQUIRED);
        }

        String identifier;
        User user;
        if (isEmail) {
            String email = request.getEmail().trim().toLowerCase();
            identifier = email;
            user = userMapper.selectByEmail(email);
        } else {
            String phone = normalizePhone(request.getPhone());
            identifier = phone;
            user = userMapper.selectByPhone(phone);
        }

        String failKey = "user:auth:login-fail:" + identifier;
        String lockKey = "user:auth:account-lock:" + identifier;

        if (cacheUtil.get(lockKey) != null) {
            throw new BusinessException(UserAuthErrorCode.OPERATION_TOO_FREQUENT);
        }

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            saveLoginLog(0L, 1, clientIp, userAgent, 0, "账号或密码错误");
            incrementLoginFail(failKey, lockKey);
            throw new BusinessException(UserAuthErrorCode.ACCOUNT_OR_PASSWORD_ERROR);
        }

        if (user.getUserStatus() == UserStatusEnum.DISABLED.getCode()) {
            throw new BusinessException(UserAuthErrorCode.ACCOUNT_DISABLED);
        }

        cacheUtil.delete(failKey);
        saveLoginLog(user.getId(), 1, clientIp, userAgent, 1, null);
        boolean rememberMe = request.getRememberMe() != null && request.getRememberMe();
        return buildAuthTokenVO(user, rememberMe);
}

    private void incrementLoginFail(String failKey, String lockKey) {
        synchronized (failKey.intern()) {
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
    }

    @Override
    public AuthTokenVO refreshToken(RefreshTokenRequest request) {
        Long userId = jwtUtil.parseRefreshToken(request.getRefreshToken());

        // 校验是否在密码重置之前签发
        // 注：JWT iat 仅保留秒级精度，重置与登录连续触发常落在同一秒。
        //   同秒情况下无法区分「重置前的旧 token」与「重置后的新 token」，
        //   此处按秒比较采用严格 <，把同秒新 token 当作有效（与同秒旧 token 的 1 秒残留窗口权衡）。
        Date resetAt = cacheUtil.get(PASSWORD_RESET_AT_PREFIX + userId);
        if (resetAt != null) {
            Date tokenIat = jwtUtil.getRefreshTokenIssuedAt(request.getRefreshToken());
            if (tokenIat == null || tokenIat.getTime() / 1000 < resetAt.getTime() / 1000) {
                throw new BusinessException(UserAuthErrorCode.REFRESH_TOKEN_INVALID);
            }
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getUserStatus() == UserStatusEnum.DISABLED.getCode()) {
            throw new BusinessException(UserAuthErrorCode.REFRESH_TOKEN_INVALID);
        }
        boolean rememberMe = jwtUtil.parseRememberMeFromRefreshToken(request.getRefreshToken());
        return buildAuthTokenVO(user, rememberMe);
    }

    @Override
    public void logout(String accessToken) {
        String jti = jwtUtil.getJti(accessToken);
        long ttlMillis = jwtUtil.getExpiration(accessToken).getTime() - System.currentTimeMillis();
        if (ttlMillis > 0) {
            cacheUtil.set("user:auth:token-blacklist:" + jti, true, ttlMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }
    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return "";
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 13 && digits.startsWith("86")) {
            return digits.substring(2);
        }
        if (digits.length() == 14 && digits.startsWith("086")) {
            return digits.substring(3);
        }
        return digits;
    }
}
