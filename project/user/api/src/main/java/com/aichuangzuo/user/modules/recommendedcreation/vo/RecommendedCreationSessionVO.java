package com.aichuangzuo.user.modules.recommendedcreation.vo;

import lombok.Data;

import java.util.List;

@Data
public class RecommendedCreationSessionVO {
    private Integer currentStep;
    private List<TopicOptionVO> topics;
    private TopicOptionVO selectedTopic;
    private List<AngleOptionVO> angles;
    private List<AngleOptionVO> selectedAngles;
    private Integer wordCount;
    private String prompt;
    private String template;
}
