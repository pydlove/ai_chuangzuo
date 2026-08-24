
package com.aichuangzuo.user.modules.auth.service.impl;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.cache.CacheUtil;
import com.aichuangzuo.user.modules.auth.service.SmsCodeService;
import com.aichuangzuo.user.modules.security.smsconfig.entity.SmsConfig;
import com.aichuangzuo.user.modules.security.smsconfig.mapper.SmsConfigMapper;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeServiceImpl implements SmsCodeService {

    private static final String SMS_CODE_PREFIX = "user:auth:sms-code:";
    private static final String SMS_LAST_SEND_PREFIX = "user:auth:sms:last-send:";
    private static final String SMS_DAILY_PHONE_PREFIX = "user:auth:sms:daily-phone:";
    private static final String SMS_DAILY_IP_PREFIX = "user:auth:sms:daily-ip:";
    private static final String SMS_DAILY_GLOBAL_PREFIX = "user:auth:sms:daily-global:";
    private static final long SMS_CODE_TTL_MINUTES = 5;
    private static final long DAILY_COUNTER_TTL_HOURS = 24;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SmsConfigMapper smsConfigMapper;
    private final CacheUtil cacheUtil;
    private final StringEncryptor encryptor;

    @Override
    public void sendSmsCode(String phone, String clientIp) {
        String normalizedPhone = normalizePhone(phone);
        if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            throw new BusinessException(UserAuthErrorCode.PHONE_FORMAT_ERROR);
        }

        SmsConfig config = smsConfigMapper.selectById(1L);
        if (config == null || config.getEnabled() == null || config.getEnabled() != 1) {
            throw new BusinessException(UserAuthErrorCode.SMS_CONFIG_NOT_ENABLED);
        }
        validateSecurityPolicy(config, normalizedPhone, clientIp);

        String code = generateCode();
        sendAliyunSms(config, normalizedPhone, code);

        cacheUtil.set(SMS_CODE_PREFIX + normalizedPhone, code, SMS_CODE_TTL_MINUTES, TimeUnit.MINUTES);
        recordSecurityUsage(config, normalizedPhone, clientIp);
        log.info("短信验证码已发送 phone={}", normalizedPhone);
    }

    @Override
    public boolean validateSmsCode(String phone, String smsCode) {
        String normalizedPhone = normalizePhone(phone);
        String key = SMS_CODE_PREFIX + normalizedPhone;
        String cachedCode = cacheUtil.get(key);
        if (cachedCode == null) {
            return false;
        }
        cacheUtil.delete(key);
        return cachedCode.equals(smsCode);
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

    private void validateSecurityPolicy(SmsConfig config, String phone, String clientIp) {
        String today = LocalDate.now().format(DATE_FORMAT);

        Long lastSend = cacheUtil.get(SMS_LAST_SEND_PREFIX + phone);
        if (lastSend != null) {
            long secondsSince = (System.currentTimeMillis() - lastSend) / 1000;
            if (secondsSince < config.getSendIntervalSeconds()) {
                throw new BusinessException(UserAuthErrorCode.OPERATION_TOO_FREQUENT);
            }
        }

        checkDailyCounter(SMS_DAILY_PHONE_PREFIX + phone + ":" + today, config.getDailyMaxPerPhone(), "phone");
        checkDailyCounter(SMS_DAILY_IP_PREFIX + clientIp + ":" + today, config.getDailyMaxPerIp(), "ip");
        checkDailyCounter(SMS_DAILY_GLOBAL_PREFIX + today, config.getGlobalDailyMax(), "global");
    }

    private void checkDailyCounter(String key, int max, String type) {
        AtomicInteger count = cacheUtil.get(key);
        if (count != null && count.get() >= max) {
            log.warn("短信日限触发 type={} key={} max={}", type, key, max);
            throw new BusinessException(UserAuthErrorCode.OPERATION_TOO_FREQUENT);
        }
    }

    private void recordSecurityUsage(SmsConfig config, String phone, String clientIp) {
        String today = LocalDate.now().format(DATE_FORMAT);
        cacheUtil.set(SMS_LAST_SEND_PREFIX + phone, System.currentTimeMillis(), config.getSendIntervalSeconds(), TimeUnit.SECONDS);
        incrementCounter(SMS_DAILY_PHONE_PREFIX + phone + ":" + today, config.getDailyMaxPerPhone());
        incrementCounter(SMS_DAILY_IP_PREFIX + clientIp + ":" + today, config.getDailyMaxPerIp());
        incrementCounter(SMS_DAILY_GLOBAL_PREFIX + today, config.getGlobalDailyMax());
    }

    private void incrementCounter(String key, int max) {
        synchronized (key.intern()) {
            AtomicInteger count = cacheUtil.get(key);
            if (count == null) {
                count = new AtomicInteger(0);
                cacheUtil.set(key, count, DAILY_COUNTER_TTL_HOURS, TimeUnit.HOURS);
            }
            if (count.get() >= max) {
                throw new BusinessException(UserAuthErrorCode.OPERATION_TOO_FREQUENT);
            }
            count.incrementAndGet();
        }
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private void sendAliyunSms(SmsConfig config, String phone, String code) {
        String accessKeySecret = decryptSecret(config.getAccessKeySecret());
        if (!StringUtils.hasText(accessKeySecret)) {
            log.warn("短信 AccessKeySecret 未配置或解密失败");
            throw new BusinessException(UserAuthErrorCode.SMS_CONFIG_NOT_ENABLED);
        }
        if (!StringUtils.hasText(config.getAccessKeyId()) || !StringUtils.hasText(config.getSignName()) || !StringUtils.hasText(config.getTemplateCode())) {
            log.warn("短信配置不完整 accessKeyId={} signName={} templateCode={}",
                    StringUtils.hasText(config.getAccessKeyId()), StringUtils.hasText(config.getSignName()), StringUtils.hasText(config.getTemplateCode()));
            throw new BusinessException(UserAuthErrorCode.SMS_CONFIG_NOT_ENABLED);
        }

        String templateParam = "{\"code\":\"" + code + "\",\"min\":\"5\"}";

        try {
            Config openapiConfig = new Config()
                    .setAccessKeyId(config.getAccessKeyId())
                    .setAccessKeySecret(accessKeySecret);
            openapiConfig.endpoint = "dypnsapi.aliyuncs.com";
            Client client = new Client(openapiConfig);

            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setSignName(config.getSignName())
                    .setTemplateCode(config.getTemplateCode())
                    .setTemplateParam(templateParam);

            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCodeWithOptions(request, new RuntimeOptions());
            log.info("阿里云短信发送响应 phone={} body={}", phone, response != null ? response.getBody() : null);
            if (response == null || response.getBody() == null || !Boolean.TRUE.equals(response.getBody().getSuccess())) {
                log.warn("阿里云短信发送失败 phone={} response={}", phone, response != null ? response.getBody() : null);
                throw new BusinessException(UserAuthErrorCode.SMS_SEND_FAILED);
            }
        } catch (Exception e) {
            log.warn("阿里云短信发送异常 phone={} message={}", phone, e.getMessage(), e);
            throw new BusinessException(UserAuthErrorCode.SMS_SEND_FAILED);
        }
    }

    private String decryptSecret(String encrypted) {
        if (!StringUtils.hasText(encrypted)) {
            return "";
        }
        try {
            return encryptor.decrypt(encrypted);
        } catch (Exception e) {
            log.warn("短信 AccessKeySecret 解密失败");
            return "";
        }
    }
}
