package com.aichuangzuo.user.modules.skill.market.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户端 - 风格市场概览数据。
 *
 * <p>包含平台统计、官方精选、收益潜力榜，供首页非分页区域展示。
 */
@Data
public class MarketSkillOverviewVO {

    /** 已上架风格总数。 */
    private Long approvedCount;

    /** 累计使用次数。 */
    private Long totalUses;

    /** 累计发放创作币。 */
    private BigDecimal totalEarnings;

    /** 官方精选（按总使用次数降序）。 */
    private List<MarketSkillVO> featuredSkills;

    /** 收益潜力榜（按本周收益降序）。 */
    private List<TopCreatorVO> topCreators;
}
