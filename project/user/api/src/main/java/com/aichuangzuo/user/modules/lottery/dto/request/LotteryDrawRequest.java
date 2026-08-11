package com.aichuangzuo.user.modules.lottery.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LotteryDrawRequest {

    @NotNull(message = "活动ID不能为空")
    private Long campaignId;
}
