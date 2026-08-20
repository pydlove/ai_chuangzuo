package com.aichuangzuo.user.modules.recommendedcreation.vo;

import lombok.Data;

@Data
public class TopicOptionVO {
    private String id;
    private String title;
    private String risk;
    private String riskLabel;
    private Integer caseCount;
    private String recommendedAngle;
}
