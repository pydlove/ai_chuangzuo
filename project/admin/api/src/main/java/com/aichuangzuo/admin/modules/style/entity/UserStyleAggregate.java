package com.aichuangzuo.admin.modules.style.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.aichuangzuo.shared.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 管理端对用户表 {@code u_user_style} 的读模型。
 *
 * <p>同 leaderboard 模块下 {@code IncomeSubmission} 的做法：管理员侧独立 Java 实体指向用户端表，
 * 字段命名严格沿用迁移脚本 V1.0.0_005 与 V1.0.0_012。
 *
 * <p>{@code u_user_style} 表不含 {@code created_by} / {@code updated_by} 列，因此这里用
 * {@code @TableField(exist = false)} 覆盖基类同名字段，避免 MyBatis-Plus 生成不存在的列。
 */
@Getter
@Setter
@TableName("u_user_style")
public class UserStyleAggregate extends BaseEntity {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务唯一编号。 */
    private String bizNo;

    /** 所属用户ID。 */
    private Long userId;

    /** 风格名称。 */
    private String styleName;

    /** 风格提示词。 */
    private String prompt;

    /** 简短描述（系统预选用）。 */
    private String description;

    /** 提示词摘要，UI 卡片展示用（系统预选用）。 */
    private String promptSummary;

    /** 适用范围标签，逗号分隔。 */
    private String scope;

    /** 启用状态：0-禁用，1-启用（仅系统预设有意义）。 */
    private Integer enableStatus;

    /** 来源类型：1-自定义，2-学习。 */
    private Integer sourceType;

    /** 累计使用次数。 */
    private Integer useCount;

    /** 审核状态：0-待审核，1-已通过，2-已拒绝。 */
    private Integer auditStatus;

    /** 审核管理员ID。 */
    private Long auditedBy;

    /** 审核时间。 */
    private LocalDateTime auditedAt;

    /** 打回原因。 */
    private String rejectReason;

    /** 表中无此列，覆盖基类字段，告知 MyBatis-Plus 忽略。 */
    @TableField(exist = false)
    private Long createdBy;

    /** 表中无此列，覆盖基类字段，告知 MyBatis-Plus 忽略。 */
    @TableField(exist = false)
    private Long updatedBy;
}