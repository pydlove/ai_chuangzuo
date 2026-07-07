package com.aichuangzuo.admin.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("a_admin_user_role_rel")
public class AdminUserRoleRel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adminUserId;

    private Long roleId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
