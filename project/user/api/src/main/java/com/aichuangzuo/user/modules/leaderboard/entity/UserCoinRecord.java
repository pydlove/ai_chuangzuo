package com.aichuangzuo.user.modules.leaderboard.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户创作币流水实体，对应表 u_user_coin_record。
 */
@Getter
@Setter
@TableName("u_user_coin_record")
public class UserCoinRecord {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务唯一编号。 */
    private String bizNo;

    /** 所属用户ID。 */
    private Long userId;

    /** 业务类型：leaderboard_reward / admin_adjust / invite_reward 等。 */
    private String bizType;

    /** 方向：1-收入，2-支出。 */
    private Integer direction;

    /** 本次金额（始终为正）。 */
    private BigDecimal amount;

    /** 本次入账后余额快照。 */
    private BigDecimal balanceAfter;

    /** 关联业务ID。 */
    private String refId;

    /** 备注。 */
    private String remark;

    /** 业务发生时间。 */
    private LocalDateTime bizTime;

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
