package com.aichuangzuo.user.modules.membership.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户会员状态实体，对应表 u_user_membership。
 */
@Getter
@Setter
@TableName("u_user_membership")
public class UserMembership {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID。 */
    private Long userId;

    /** 当前等级：basic/pro/flagship。 */
    private String level;

    /** 本次会员开始日期。 */
    private LocalDate startedAt;

    /** 会员到期日期。 */
    private LocalDate expiresAt;

    /** 租户ID。 */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;
}
