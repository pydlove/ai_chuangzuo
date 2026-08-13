package com.aichuangzuo.admin.modules.leaderboard.config.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 收益排行榜奖励规则配置更新请求。
 */
@Data
public class LeaderboardRewardConfigUpdateRequest {

    @NotNull(message = "奖励名次上限不能为空")
    @Min(value = 1, message = "奖励名次上限至少为 1")
    private Integer topLimit;

    @NotNull(message = "奖励金额不能为空")
    @DecimalMin(value = "0.0001", message = "奖励金额必须大于 0")
    private BigDecimal rewardAmount;
}
