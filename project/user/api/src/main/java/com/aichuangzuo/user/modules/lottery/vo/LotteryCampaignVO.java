package com.aichuangzuo.user.modules.lottery.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LotteryCampaignVO {

    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private String rules;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<LotteryPrizeTierVO> tiers;
}
