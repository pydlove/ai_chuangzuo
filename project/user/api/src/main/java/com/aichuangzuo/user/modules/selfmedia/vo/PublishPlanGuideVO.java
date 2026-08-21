package com.aichuangzuo.user.modules.selfmedia.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PublishPlanGuideVO {

    private MainPlatformPlan mainPlatform;
    private ColdStartPlan coldStart;
    private List<RepostPlan> reposts;

    @Data
    public static class MainPlatformPlan {
        private String platform;
        private String publishTime;
        private String reason;
    }

    @Data
    public static class ColdStartPlan {
        private List<String> immediateActions;
        private String duration;
        private String sharingTips;
    }

    @Data
    public static class RepostPlan {
        private String platform;
        private String publishTime;
        private String title;
        private List<String> tags;
        private String imageSuggestions;
        private String tips;
    }

    public static PublishPlanGuideVO empty() {
        PublishPlanGuideVO vo = new PublishPlanGuideVO();
        vo.setMainPlatform(new MainPlatformPlan());
        vo.setReposts(new ArrayList<>());
        return vo;
    }
}
