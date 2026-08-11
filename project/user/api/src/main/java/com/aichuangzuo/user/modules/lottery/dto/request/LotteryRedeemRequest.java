package com.aichuangzuo.user.modules.lottery.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LotteryRedeemRequest {

    @NotBlank(message = "兑换码不能为空")
    private String code;
}
