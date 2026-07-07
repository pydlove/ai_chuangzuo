package com.aichuangzuo.admin.modules.leaderboard.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端发奖请求。
 */
@Data
public class LeaderboardGrantRequest {

    @NotNull
    private Integer leaderboardType;

    @NotBlank
    private String periodMonth;
}
