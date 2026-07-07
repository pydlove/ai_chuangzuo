package com.aichuangzuo.admin.modules.leaderboard.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自媒体收入申报记录实体，对应表 u_leaderboard_income_submission。
 */
@Getter
@Setter
@TableName("u_leaderboard_income_submission")
public class IncomeSubmission {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务唯一编号。 */
    private String bizNo;

    /** 申报用户ID。 */
    private Long userId;

    /** 申报所属月份，格式 YYYY-MM。 */
    private String periodMonth;

    /** 申报金额（元）。 */
    private BigDecimal amount;

    /** 自媒体平台：wechat / xiaohongshu / douyin / other。 */
    private String platform;

    /** 收益截图本地路径列表（JSON 数组）。 */
    private String screenshotPaths;

    /** 审核状态：0-待审核，1-已通过，2-已拒绝。 */
    private Integer auditStatus;

    /** 审核管理员ID。 */
    private Long auditedBy;

    /** 审核时间。 */
    private LocalDateTime auditedAt;

    /** 拒绝原因。 */
    private String rejectReason;

    /** 租户ID。 */
    private Long tenantId;

    /** 逻辑删除标记。 */
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
