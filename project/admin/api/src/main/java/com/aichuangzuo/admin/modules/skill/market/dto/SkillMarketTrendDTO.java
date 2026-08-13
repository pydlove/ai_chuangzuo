package com.aichuangzuo.admin.modules.skill.market.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 提示词市场使用趋势行。
 */
@Data
public class SkillMarketTrendDTO {
    private String date;
    private Long uses;
    private BigDecimal earnings;
}
