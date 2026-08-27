package com.aichuangzuo.user.modules.auth.vo;

import lombok.Data;

@Data
public class QrLoginCreateVO {
    private String qrCode;
    private String qrUrl;
    private Integer expiresIn;
}
