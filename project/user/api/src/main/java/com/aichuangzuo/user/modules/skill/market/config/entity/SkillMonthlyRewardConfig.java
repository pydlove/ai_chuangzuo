package com.aichuangzuo.user.modules.skill.market.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提示词市场月度排行榜奖励配置，对应表 {@code a_skill_monthly_reward_config}。
 *
 * <p>由 admin 端维护，user 端月结 job 读取。
 */
@Data
@TableName("a_skill_monthly_reward_config")
public class SkillMonthlyRewardConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private BigDecimal firstAmount;
    private BigDecimal secondAmount;
    private BigDecimal thirdAmount;
    private BigDecimal fourthAmount;
    private BigDecimal fifthAmount;
    private String settlementCron;
    private Integer enabled;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
