package com.aichuangzuo.user.modules.auth.service;

public interface EmailCodeService {
    void sendEmailCode(String email, String captchaKey, String captchaCode);
    boolean validateEmailCode(String email, String emailCode);
}
