package com.aichuangzuo.user.modules.auth.vo;

import lombok.Data;

@Data
public class AuthTokenVO {
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn;
    private UserVO user;
}
