package com.aichuangzuo.user.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizNo;
    private String nickname;
    private String email;
    private String passwordHash;
    private String avatarUrl;
    private String inviteCode;
    private Integer userStatus;
    private Integer emailVerified;
    private Long tenantId;
    @TableLogic
    private Integer isDeleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
}
