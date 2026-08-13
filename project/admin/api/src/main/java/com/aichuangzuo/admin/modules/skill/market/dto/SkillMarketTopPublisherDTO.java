package com.aichuangzuo.admin.modules.skill.market.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 热门发布者排行行。
 */
@Data
public class SkillMarketTopPublisherDTO {
    private Long publisherUserId;
    private String publisherName;
    private Long skillCount;
    private Long totalUses;
    private BigDecimal weeklyEarnings;
}
