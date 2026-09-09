
package com.aichuangzuo.user.modules.auth.service;

public interface SmsCodeService {
    void sendSmsCode(String phone, String clientIp, String scene);
    boolean validateSmsCode(String phone, String smsCode);
}
