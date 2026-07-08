package com.aichuangzuo.user.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体，对应表 u_user。
 * 字段命名严格沿用表 V1.0.0_001 迁移脚本；本类只承载持久化结构，
 * 业务校验在 service 层（如昵称长度、邮箱格式等），不在这里用注解约束。
 */
@Getter
@Setter
@TableName("u_user")
public class User {
    /** 主键ID；雪花之外也兼容 AUTO_INCREMENT，由 MyBatis-Plus 自动填充。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户对外暴露的业务编号，如 U000123；注册时生成，不可变。 */
    private String bizNo;
    /** 用户昵称；1-20 字符，service 层校验。 */
    private String nickname;
    /** 登录邮箱；唯一约束由 uk_u_user_email 保证；改邮箱时同时清空旧 email 验证码缓存。 */
    private String email;
    /** BCrypt 加密后的密码哈希；原始密码不存。 */
    private String passwordHash;
    /** 头像 URL；可为空，UI 层空时回退到昵称首字母。 */
    private String avatarUrl;
    /** 个人邀请码 6 位；唯一约束由 uk_u_user_invite_code 保证；注册时生成。 */
    private String inviteCode;
    /** 创作币余额。 */
    private BigDecimal coinBalance;
    /** 用户状态：0-禁用 / 1-正常；禁用时 JwtAuthenticationFilter 仍能解析 token 但登录接口拒绝。 */
    private Integer userStatus;
    /** 用户类型：0-机器人 / 1-真实用户。 */
    private Integer userType;
    /** 邮箱是否验证：0-否 / 1-是；改邮箱成功后置 1。 */
    private Integer emailVerified;
    /** 租户ID；当前统一为 0，预留多租户扩展。 */
    private Long tenantId;
    /** 逻辑删除标记：0-未删 / 1-已删；所有查询自动追加 is_deleted=0 条件。 */
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