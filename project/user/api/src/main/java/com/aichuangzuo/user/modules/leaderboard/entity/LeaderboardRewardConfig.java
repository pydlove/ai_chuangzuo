package com.aichuangzuo.user.modules.leaderboard.entity;

import com.aichuangzuo.shared.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 收益排行榜奖励规则配置，对应 admin schema 表 {@code a_leaderboard_reward_config}。
 *
 * <p>用户端只读，配置由管理端维护。</p>
 */
@Getter
@Setter
@TableName("a_leaderboard_reward_config")
public class LeaderboardRewardConfig extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 可发放奖励的名次上限，如 TOP 3。 */
    private Integer rewardTopLimit;

    /** 每名奖励的创作币数量。 */
    private BigDecimal rewardAmount;
}
