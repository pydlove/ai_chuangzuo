package com.aichuangzuo.user.modules.lottery.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LotteryDrawResultVO {

    private Long tierId;
    private String tierName;
    private String rewardType;
    private String rewardValueJson;
    private String code;
    private LocalDateTime codeExpiresAt;
    private String message;
}
