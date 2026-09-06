package com.aichuangzuo.admin.modules.lottery.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LotteryManualGrantRequest {

    @NotNull(message = "活动ID不能为空")
    private Long campaignId;

    @NotNull(message = "奖项ID不能为空")
    private Long tierId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
