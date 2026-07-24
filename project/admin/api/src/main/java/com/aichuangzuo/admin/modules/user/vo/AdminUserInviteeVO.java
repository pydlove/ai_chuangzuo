package com.aichuangzuo.admin.modules.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端邀请关系中的用户简要信息。
 */
@Data
public class AdminUserInviteeVO {

    private Long id;
    private String email;
    private String nickname;
    private LocalDateTime createdAt;
}
