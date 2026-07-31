package com.aichuangzuo.admin.modules.skill.market.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提示词市场月度排行榜奖励配置，对应表 {@code a_skill_monthly_reward_config}。
 *
 * <p>单行配置（id=1），由 admin 端 GET/PUT 维护。
 */
@Getter
@Setter
@TableName("a_skill_monthly_reward_config")
public class SkillMonthlyRewardConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** Top1 奖励金额。 */
    private BigDecimal firstAmount;

    /** Top2 奖励金额。 */
    private BigDecimal secondAmount;

    /** Top3 奖励金额。 */
    private BigDecimal thirdAmount;

    /** Top4 奖励金额。 */
    private BigDecimal fourthAmount;

    /** Top5 奖励金额。 */
    private BigDecimal fifthAmount;

    /** 月结定时任务 cron 表达式。 */
    private String settlementCron;

    /** 是否启用。 */
    private Integer enabled;

    /** 提示词每次被使用创作者获得的收益（创作币），统一配置。 */
    private BigDecimal pricePerUse;

    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
