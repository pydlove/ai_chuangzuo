package com.aichuangzuo.admin.modules.leaderboard.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 收入申报拒绝请求。
 */
@Data
public class LeaderboardRejectRequest {

    @NotBlank
    private String reason;
}
