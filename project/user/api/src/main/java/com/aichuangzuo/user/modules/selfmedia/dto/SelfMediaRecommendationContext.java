package com.aichuangzuo.user.modules.selfmedia.dto;

import lombok.Data;

@Data
public class SelfMediaRecommendationContext {
    private String workType;
    private String timePerWeek;
    private String incomeGoal;
    private String breakEvenPeriod;
    private String contentType;
    private String audience;
    private String identity;
    private String onCamera;
    private String note;
}
