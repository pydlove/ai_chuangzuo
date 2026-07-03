package com.aichuangzuo.user.modules.auth.vo;

import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String bizNo;
    private String nickname;
    private String email;
    private String avatarUrl;
}
