package com.aichuangzuo.admin.modules.leaderboard.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 榜单奖励发放记录实体，对应表 u_leaderboard_reward_record。
 */
@Getter
@Setter
@TableName("u_leaderboard_reward_record")
public class RewardRecord {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务唯一编号。 */
    private String bizNo;

    /** 榜单类型：1-创作币榜，2-自媒体收入榜。 */
    private Integer leaderboardType;

    /** 榜单所属月份，格式 YYYY-MM。 */
    private String periodMonth;

    /** 排名。 */
    private Integer rankNo;

    /** 获奖用户ID。 */
    private Long userId;

    /** 奖励金额（创作币）。 */
    private BigDecimal amount;

    /** 对应用户端创作币流水业务编号。 */
    private String coinRecordBizNo;

    /** 发放管理员ID。 */
    private Long grantedBy;

    /** 发放时间。 */
    private LocalDateTime grantedAt;

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
