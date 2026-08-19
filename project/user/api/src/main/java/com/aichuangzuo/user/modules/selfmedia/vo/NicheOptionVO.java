package com.aichuangzuo.user.modules.selfmedia.vo;

import lombok.Data;

@Data
public class NicheOptionVO {
    private String key;
    private String name;
    private String audience;
    private String monetization;
    private String riskLabel;
    private String riskColor;
    private Integer caseCount;
    private String reason;
}
