package com.aichuangzuo.admin.modules.skill.market.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 提示词市场统计概览 VO。
 */
@Data
public class MarketSkillStatsVO {
    private Overview overview;
    private List<TopSkill> topSkills;
    private List<TopPublisher> topPublishers;
    private List<TrendItem> usageTrend;

    @Data
    public static class Overview {
        private Long totalSkills;
        private Long enabledSkills;
        private Long disabledSkills;
        private Long featuredSkills;
        private Long totalUses;
        private Long weeklyUses;
        private BigDecimal totalEarnings;
        private BigDecimal weeklyEarnings;
        private BigDecimal milestoneBonus;
    }

    @Data
    public static class TopSkill {
        private String skillName;
        private String publisherName;
        private Long publisherUserId;
        private Integer totalUses;
        private Integer weeklyUses;
        private BigDecimal weeklyEarnings;
        private BigDecimal milestoneBonus;
    }

    @Data
    public static class TopPublisher {
        private Long publisherUserId;
        private String publisherName;
        private Long skillCount;
        private Long totalUses;
        private BigDecimal weeklyEarnings;
    }

    @Data
    public static class TrendItem {
        private String date;
        private Long uses;
        private BigDecimal earnings;
    }
}
