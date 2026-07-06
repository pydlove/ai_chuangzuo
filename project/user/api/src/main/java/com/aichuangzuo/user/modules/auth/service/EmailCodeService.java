package com.aichuangzuo.user.modules.auth.service;

public interface EmailCodeService {
    void sendEmailCode(String email);
    boolean validateEmailCode(String email, String emailCode);
}
