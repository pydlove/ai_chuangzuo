package com.aichuangzuo.user.modules.auth.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.aichuangzuo.user.infrastructure.cache.CacheUtil;
import com.aichuangzuo.user.modules.auth.service.CaptchaService;
import com.aichuangzuo.user.modules.auth.vo.CaptchaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private final CacheUtil cacheUtil;

    @Value("${auth.captcha.mock-enabled:false}")
    private boolean mockEnabled;
    @Value("${auth.captcha.mock-code:MOCK}")
    private String mockCode;

    private static final String CAPTCHA_KEY_PREFIX = "user:auth:captcha:";
    private static final long CAPTCHA_TTL_MINUTES = 5;

    @Override
    public CaptchaVO generateCaptcha() {
        String captchaKey = UUID.randomUUID().toString();
        String code;
        String imageBase64;
        if (mockEnabled) {
            code = mockCode;
            imageBase64 = generateMockCaptchaImage(code);
        } else {
            LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
            code = captcha.getCode().toUpperCase();
            imageBase64 = captcha.getImageBase64();
        }
        cacheUtil.set(CAPTCHA_KEY_PREFIX + captchaKey, code, CAPTCHA_TTL_MINUTES, TimeUnit.MINUTES);

        CaptchaVO vo = new CaptchaVO();
        vo.setCaptchaKey(captchaKey);
        vo.setCaptchaImage("data:image/png;base64," + imageBase64);
        return vo;
    }

    private String generateMockCaptchaImage(String code) {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, code.length(), 0);
        return captcha.getImageBase64();
    }

    @Override
    public boolean validateCaptcha(String captchaKey, String captchaCode) {
        String key = CAPTCHA_KEY_PREFIX + captchaKey;
        String cachedCode = cacheUtil.get(key);
        if (cachedCode == null) {
            return false;
        }
        cacheUtil.delete(key);
        return cachedCode.equalsIgnoreCase(captchaCode);
    }
}
