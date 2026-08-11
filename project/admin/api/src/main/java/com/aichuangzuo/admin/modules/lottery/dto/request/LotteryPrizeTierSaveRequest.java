package com.aichuangzuo.admin.modules.lottery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LotteryPrizeTierSaveRequest {

    private Long id;

    @NotBlank(message = "奖项标识不能为空")
    private String tierKey;

    @NotBlank(message = "奖项名称不能为空")
    private String tierName;

    @NotNull(message = "概率不能为空")
    private BigDecimal probability;

    private Integer maxWinCount;

    @NotBlank(message = "奖励类型不能为空")
    private String rewardType;

    @NotBlank(message = "奖励参数不能为空")
    private String rewardValueJson;

    private String codePrefix;
    private Integer codeLength;

    @NotNull(message = "有效期天数不能为空")
    private Integer codeValidityDays;

    private Integer sortOrder;
}
