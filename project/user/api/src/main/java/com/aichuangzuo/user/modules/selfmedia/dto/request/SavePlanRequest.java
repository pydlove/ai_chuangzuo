package com.aichuangzuo.user.modules.selfmedia.dto.request;

import com.aichuangzuo.user.modules.selfmedia.dto.SelfMediaRecommendationContext;
import com.aichuangzuo.user.modules.selfmedia.vo.PillarVO;
import lombok.Data;

import java.util.List;

@Data
public class SavePlanRequest {
    private String platformKey;
    private String platformName;
    private String goal;
    private String background;
    private Boolean hasProduct;
    private String productDesc;
    private String nicheKey;
    private String nicheName;
    private String personaKey;
    private String personaName;
    private List<PillarVO> pillars;
    private Boolean isRecommendedByAI;
    private SelfMediaRecommendationContext recommendationContext;
}
