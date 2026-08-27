package com.aichuangzuo.user.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QrLoginAuthorizeRequest {
    @NotBlank(message = "二维码不能为空")
    private String qrCode;
}
