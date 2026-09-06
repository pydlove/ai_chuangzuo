package com.aichuangzuo.admin.modules.lottery.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LotteryManualChangeUserRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
