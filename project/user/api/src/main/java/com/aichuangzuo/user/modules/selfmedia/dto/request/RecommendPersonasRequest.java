package com.aichuangzuo.user.modules.selfmedia.dto.request;

import com.aichuangzuo.user.modules.selfmedia.dto.SelfMediaRecommendationContext;
import lombok.Data;

@Data
public class RecommendPersonasRequest {
    private String platformKey;
    private String goal;
    private String background;
    private String nicheKey;
    private String nicheName;
    private SelfMediaRecommendationContext context;
}
