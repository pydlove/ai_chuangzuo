package com.aichuangzuo.user.modules.auth.vo;

import lombok.Data;

@Data
public class QrLoginStatusVO {
    private String qrCode;
    private Integer status;
    private String statusLabel;
    private String scannerNickname;
    private Integer expiresIn;
}
