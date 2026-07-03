package com.aichuangzuo.user.modules.auth.service;

import com.aichuangzuo.user.modules.auth.vo.CaptchaVO;

public interface CaptchaService {
    CaptchaVO generateCaptcha();
    boolean validateCaptcha(String captchaKey, String captchaCode);
}
