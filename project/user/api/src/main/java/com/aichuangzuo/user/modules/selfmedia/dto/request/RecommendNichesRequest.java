package com.aichuangzuo.user.modules.selfmedia.dto.request;

import com.aichuangzuo.user.modules.selfmedia.dto.SelfMediaRecommendationContext;
import lombok.Data;

@Data
public class RecommendNichesRequest {
    private String platformKey;
    private String goal;
    private String background;
    private Boolean hasProduct;
    private String productDesc;
    private SelfMediaRecommendationContext context;
}
