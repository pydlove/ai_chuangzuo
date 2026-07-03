package com.aichuangzuo.user.modules.auth.service.impl;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.cache.CacheUtil;
import com.aichuangzuo.user.modules.auth.service.CaptchaService;
import com.aichuangzuo.user.modules.auth.service.EmailCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCodeServiceImpl implements EmailCodeService {

    private final CaptchaService captchaService;
    private final CacheUtil cacheUtil;

    @Value("${auth.email-code.mock-enabled:false}")
    private boolean mockEnabled;
    @Value("${auth.email-code.mock-code:}")
    private String mockCode;

    private static final String EMAIL_CODE_PREFIX = "user:auth:email-code:";
    private static final String EMAIL_CODE_COUNT_PREFIX = "user:auth:email-code-count:";
    private static final long EMAIL_CODE_TTL_MINUTES = 5;
    private static final long EMAIL_CODE_COUNT_TTL_HOURS = 24;
    private static final int MAX_EMAIL_CODE_PER_EMAIL = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void sendEmailCode(String email, String captchaKey, String captchaCode) {
        if (!captchaService.validateCaptcha(captchaKey, captchaCode)) {
            throw new BusinessException(UserAuthErrorCode.CAPTCHA_ERROR);
        }
        checkEmailCodeLimit(email);
        String code = mockEnabled ? mockCode : generateCode();
        cacheUtil.set(EMAIL_CODE_PREFIX + email, code, EMAIL_CODE_TTL_MINUTES, TimeUnit.MINUTES);
        incrementEmailCodeCount(email);
        // 本期先 mock：控制台输出，后续接入真实邮件服务
        log.info("发送邮箱验证码到 {}: {}", email, code);
    }

    @Override
    public boolean validateEmailCode(String email, String emailCode) {
        String key = EMAIL_CODE_PREFIX + email;
        String cachedCode = cacheUtil.get(key);
        if (cachedCode == null) {
            return false;
        }
        cacheUtil.delete(key);
        return cachedCode.equalsIgnoreCase(emailCode);
    }

    private void checkEmailCodeLimit(String email) {
        AtomicInteger count = cacheUtil.get(EMAIL_CODE_COUNT_PREFIX + email);
        if (count != null && count.get() >= MAX_EMAIL_CODE_PER_EMAIL) {
            throw new BusinessException(UserAuthErrorCode.OPERATION_TOO_FREQUENT);
        }
    }

    private void incrementEmailCodeCount(String email) {
        String key = EMAIL_CODE_COUNT_PREFIX + email;
        AtomicInteger count = cacheUtil.get(key);
        if (count == null) {
            count = new AtomicInteger(0);
            cacheUtil.set(key, count, EMAIL_CODE_COUNT_TTL_HOURS, TimeUnit.HOURS);
        }
        count.incrementAndGet();
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
