package com.aichuangzuo.admin.modules.plan.vo;

import com.aichuangzuo.admin.modules.plan.entity.Plan;
import lombok.Data;

@Data
public class PlanVO {
    private Long id;
    private String planKey;
    private String displayName;
    private Integer sortOrder;
    private Integer recommended;
    private java.math.BigDecimal priceMonthly;
    private java.math.BigDecimal priceQuarter;
    private java.math.BigDecimal priceYear;
    private java.math.BigDecimal originalMonthly;
    private java.math.BigDecimal originalQuarter;
    private java.math.BigDecimal originalYear;
    private String articlesMonthly;
    private String articlesQuarter;
    private String articlesYear;
    private java.math.BigDecimal savingsYear;
    private Integer status;

    public static PlanVO from(Plan p) {
        PlanVO vo = new PlanVO();
        vo.setId(p.getId());
        vo.setPlanKey(p.getPlanKey());
        vo.setDisplayName(p.getDisplayName());
        vo.setSortOrder(p.getSortOrder());
        vo.setRecommended(p.getRecommended());
        vo.setPriceMonthly(p.getPriceMonthly());
        vo.setPriceQuarter(p.getPriceQuarter());
        vo.setPriceYear(p.getPriceYear());
        vo.setOriginalMonthly(p.getOriginalMonthly());
        vo.setOriginalQuarter(p.getOriginalQuarter());
        vo.setOriginalYear(p.getOriginalYear());
        vo.setArticlesMonthly(p.getArticlesMonthly());
        vo.setArticlesQuarter(p.getArticlesQuarter());
        vo.setArticlesYear(p.getArticlesYear());
        vo.setSavingsYear(p.getSavingsYear());
        vo.setStatus(p.getStatus());
        return vo;
    }
}