package com.aichuangzuo.user.modules.lottery.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LotteryPrizeTierVO {

    private Long id;
    private String tierKey;
    private String tierName;
    private String rewardType;
    private String rewardValueJson;
    private Integer sortOrder;
}
