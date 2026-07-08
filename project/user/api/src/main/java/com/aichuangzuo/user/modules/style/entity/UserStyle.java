package com.aichuangzuo.user.modules.style.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户风格实体，对应表 u_user_style。
 *
 * <p>字段命名严格沿用迁移脚本 V1.0.0_005；
 * 通过 {@code source_type} 区分自定义风格（1）与学习的风格（2）。
 */
@Getter
@Setter
@TableName("u_user_style")
public class UserStyle {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务唯一编号，对外暴露。 */
    private String bizNo;

    /** 所属用户ID。 */
    private Long userId;

    /** 风格名称；同一用户下唯一。 */
    private String styleName;

    /** 风格提示词，生成文章时使用。 */
    private String prompt;

    /** 简短描述（系统预选用）。 */
    private String description;

    /** 提示词摘要，UI 卡片展示用（系统预选用）。 */
    private String promptSummary;

    /** 适用范围标签，逗号分隔。 */
    private String scope;

    /** 启用状态：0-禁用，1-启用（仅系统预设有意义）。 */
    private Integer enableStatus;

    /** 来源类型：1-自定义，2-学习，3-系统预设。 */
    private Integer sourceType;

    /** 累计使用次数。 */
    private Integer useCount;

    /** 逻辑删除标记：0-未删，1-已删。 */
    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 审核状态：0-待审核，1-已通过，2-已拒绝。 */
    private Integer auditStatus;

    /** 审核管理员ID。 */
    private Long auditedBy;

    /** 审核时间。 */
    private LocalDateTime auditedAt;

    /** 打回原因。 */
    private String rejectReason;
}
