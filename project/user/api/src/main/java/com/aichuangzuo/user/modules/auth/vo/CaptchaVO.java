package com.aichuangzuo.user.modules.auth.vo;

import lombok.Data;

@Data
public class CaptchaVO {
    private String captchaKey;
    private String captchaImage;
}
