package com.aichuangzuo.admin.modules.auth.vo;

import lombok.Data;

@Data
public class AdminAuthTokenVO {

    private String accessToken;

    private String refreshToken;

    private Integer expiresIn;

    private AdminUserVO user;
}
