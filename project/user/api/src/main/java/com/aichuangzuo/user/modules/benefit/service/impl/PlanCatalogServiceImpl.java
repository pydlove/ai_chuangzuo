package com.aichuangzuo.user.modules.benefit.service.impl;

import com.aichuangzuo.user.modules.benefit.entity.Benefit;
import com.aichuangzuo.user.modules.benefit.entity.PlanBenefit;
import com.aichuangzuo.user.modules.benefit.mapper.BenefitMapper;
import com.aichuangzuo.user.modules.benefit.mapper.PlanBenefitMapper;
import com.aichuangzuo.user.modules.benefit.service.PlanCatalogService;
import com.aichuangzuo.user.modules.benefit.vo.PlanCatalogVO;
import com.aichuangzuo.user.modules.membership.entity.Plan;
import com.aichuangzuo.user.modules.membership.mapper.PlanMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 套餐目录组装服务。从 u_plan / u_benefit / u_plan_benefit 三表读，
 * 渲染成定价页所需的 plans + features + compareRows。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanCatalogServiceImpl implements PlanCatalogService {

    private static final int ACTIVE = 1;
    private static final String TYPE_BOOLEAN = "boolean";
    private static final String TYPE_QUOTA = "quota";
    private static final String TYPE_TIER = "tier";
    private static final String TIER_NONE = "none";

    private final PlanMapper planMapper;
    private final BenefitMapper benefitMapper;
    private final PlanBenefitMapper planBenefitMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Cacheable(cacheNames = "planCatalog", key = "'v1'")
    public PlanCatalogVO getCatalog() {
        log.debug("组装定价目录（DB）");

        List<Plan> plans = activePlans();
        Map<String, Plan> planByKey = plans.stream().collect(Collectors.toMap(Plan::getPlanKey, Function.identity()));

        List<Benefit> benefits = activeBenefits();
        Map<String, Benefit> benefitByCode = benefits.stream().collect(Collectors.toMap(Benefit::getCode, Function.identity()));

        Map<String, Map<String, String>> valueByPlan = loadValueByPlan();

        PlanCatalogVO vo = new PlanCatalogVO();
        vo.setPlans(renderPlans(plans, benefits, valueByPlan, benefitByCode));
        vo.setCompareRows(renderCompareRows(benefits, valueByPlan, planByKey));
        return vo;
    }

    // ── plan 卡片渲染 ──

    private List<PlanCatalogVO.PlanVO> renderPlans(List<Plan> plans, List<Benefit> benefits,
            Map<String, Map<String, String>> valueByPlan, Map<String, Benefit> benefitByCode) {
        List<PlanCatalogVO.PlanVO> out = new ArrayList<>();
        for (Plan plan : plans) {
            PlanCatalogVO.PlanVO vo = new PlanCatalogVO.PlanVO();
            vo.setKey(plan.getPlanKey());
            vo.setName(plan.getDisplayName());
            vo.setRecommended(plan.getRecommended() != null && plan.getRecommended() == 1);
            vo.setMonthly(buildBlock(plan.getPriceMonthly(), plan.getOriginalMonthly(), plan.getArticlesMonthly(), null));
            vo.setQuarter(buildBlock(plan.getPriceQuarter(), plan.getOriginalQuarter(), plan.getArticlesQuarter(), null));
            vo.setYear(buildBlock(plan.getPriceYear(), plan.getOriginalYear(), plan.getArticlesYear(), plan.getSavingsYear()));
            vo.setFeatures(renderFeaturesForPlan(plan.getPlanKey(), benefits, valueByPlan, benefitByCode));
            out.add(vo);
        }
        return out;
    }

    private PlanCatalogVO.PriceBlock buildBlock(BigDecimal current, BigDecimal original, String articles, BigDecimal savings) {
        PlanCatalogVO.PriceBlock block = new PlanCatalogVO.PriceBlock();
        block.setCurrent(current);
        block.setOriginal(original);
        block.setArticles(articles);
        block.setSavings(savings);
        return block;
    }

    private List<PlanCatalogVO.FeatureVO> renderFeaturesForPlan(String planKey, List<Benefit> benefits,
            Map<String, Map<String, String>> valueByPlan, Map<String, Benefit> benefitByCode) {
        Map<String, String> values = valueByPlan.getOrDefault(planKey, Collections.emptyMap());
        List<PlanCatalogVO.FeatureVO> out = new ArrayList<>();
        for (Benefit benefit : benefits) {
            String label = benefit.getDisplayLabel();
            if (!StringUtils.hasText(label)) {
                continue;
            }
            String value = values.get(benefit.getCode());
            String text;
            boolean included;
            if (TYPE_BOOLEAN.equals(benefit.getType())) {
                text = label;
                included = "true".equalsIgnoreCase(value);
            } else if (TYPE_QUOTA.equals(benefit.getType())) {
                // valueLabelJson 覆盖视为包含（-1 → 永久 等特殊值不算 0 配额）
                String labelOverride = value != null ? lookupValueLabel(benefit, value) : null;
                if (labelOverride != null) {
                    included = true;
                    text = labelOverride;
                } else {
                    int n = parseInt(value, 0);
                    included = n > 0;
                    text = included ? renderTemplate(benefit.getCardValueTpl(), String.valueOf(n), label) : label;
                }
            } else if (TYPE_TIER.equals(benefit.getType())) {
                String labelText = lookupValueLabel(benefit, value);
                included = !TIER_NONE.equalsIgnoreCase(value);
                text = labelText != null ? labelText : label;
            } else {
                text = label;
                included = value != null;
            }
            PlanCatalogVO.FeatureVO feature = new PlanCatalogVO.FeatureVO();
            feature.setCode(benefit.getCode());
            feature.setText(text);
            feature.setIncluded(included);
            out.add(feature);
        }
        return out;
    }

    // ── 对比表渲染 ──

    private List<PlanCatalogVO.CompareRowVO> renderCompareRows(List<Benefit> benefits,
            Map<String, Map<String, String>> valueByPlan, Map<String, Plan> planByKey) {
        List<PlanCatalogVO.CompareRowVO> out = new ArrayList<>();
        for (Benefit benefit : benefits) {
            if (!StringUtils.hasText(benefit.getDisplayLabel())) {
                continue;
            }
            PlanCatalogVO.CompareRowVO row = new PlanCatalogVO.CompareRowVO();
            row.setCode(benefit.getCode());
            row.setLabel(benefit.getDisplayLabel());
            row.setBasic(renderCell(benefit, valueByPlan.getOrDefault("basic", Collections.emptyMap()).get(benefit.getCode())));
            row.setPro(renderCell(benefit, valueByPlan.getOrDefault("pro", Collections.emptyMap()).get(benefit.getCode())));
            row.setFlagship(renderCell(benefit, valueByPlan.getOrDefault("flagship", Collections.emptyMap()).get(benefit.getCode())));
            out.add(row);
        }
        return out;
    }

    private PlanCatalogVO.CompareCell renderCell(Benefit benefit, String value) {
        if (TYPE_BOOLEAN.equals(benefit.getType())) {
            return new PlanCatalogVO.CompareCell(Boolean.parseBoolean(value));
        }
        if (TYPE_QUOTA.equals(benefit.getType())) {
            // 优先查 valueLabelJson 覆盖（特殊值如 -1→永久，非数值含义）
            String labelOverride = value != null ? lookupValueLabel(benefit, value) : null;
            if (labelOverride != null) {
                return new PlanCatalogVO.CompareCell(labelOverride);
            }
            int n = parseInt(value, 0);
            if (n <= 0) {
                return new PlanCatalogVO.CompareCell(Boolean.FALSE);
            }
            String rendered = renderTemplate(benefit.getCardValueTpl(), String.valueOf(n), benefit.getDisplayLabel());
            // quota 卡片模板（如 {value} 篇/月）通常已包含数值，去掉冗余后缀
            String compact = rendered.endsWith(benefit.getDisplayLabel())
                    ? rendered.substring(0, rendered.length() - benefit.getDisplayLabel().length()).trim()
                    : rendered;
            return new PlanCatalogVO.CompareCell(compact);
        }
        if (TYPE_TIER.equals(benefit.getType())) {
            if (TIER_NONE.equalsIgnoreCase(value)) {
                return new PlanCatalogVO.CompareCell(Boolean.FALSE);
            }
            String labelText = lookupValueLabel(benefit, value);
            return new PlanCatalogVO.CompareCell(labelText != null ? labelText : value);
        }
        return new PlanCatalogVO.CompareCell(value);
    }

    // ── helpers ──

    private List<Plan> activePlans() {
        return planMapper.selectList(new LambdaQueryWrapper<Plan>()
                .eq(Plan::getStatus, ACTIVE)
                .orderByAsc(Plan::getSortOrder));
    }

    private List<Benefit> activeBenefits() {
        return benefitMapper.selectList(new LambdaQueryWrapper<Benefit>()
                .eq(Benefit::getStatus, ACTIVE)
                .orderByAsc(Benefit::getSortOrder));
    }

    /** planKey → (benefitCode → value) */
    private Map<String, Map<String, String>> loadValueByPlan() {
        Map<String, Map<String, String>> result = new HashMap<>();
        for (PlanBenefit pb : planBenefitMapper.selectList(null)) {
            result.computeIfAbsent(pb.getPlanKey(), k -> new HashMap<>())
                    .put(pb.getBenefitCode(), pb.getBenefitValue());
        }
        return result;
    }

    /**
     * quota 卡片的展示文本：优先用 valueLabelJson 覆盖（特殊值如 -1→永久），
     * 否则用 cardValueTpl 模板。
     */
    private String renderQuotaText(Benefit benefit, String value, String fallbackLabel) {
        String override = lookupValueLabel(benefit, value);
        if (override != null) {
            return override;
        }
        return renderTemplate(benefit.getCardValueTpl(), value, fallbackLabel);
    }

    /**
     * 用 {value} 占位符渲染模板；模板为空时回退 "{value}"；缺占位符时附加 "{value}"。
     */
    private String renderTemplate(String template, String value, String fallbackLabel) {
        if (!StringUtils.hasText(template)) {
            return value;
        }
        return template.replace("{value}", value);
    }

    /** 通过 valueLabelJson 覆盖；JSON 为空或缺键时返回 null。 */
    private String lookupValueLabel(Benefit benefit, String value) {
        if (!StringUtils.hasText(benefit.getValueLabelJson()) || value == null) {
            return null;
        }
        try {
            Map<String, String> map = objectMapper.readValue(benefit.getValueLabelJson(), new TypeReference<>() {});
            return map.get(value);
        } catch (Exception e) {
            log.warn("valueLabelJson 解析失败 code={}, json={}", benefit.getCode(), benefit.getValueLabelJson(), e);
            return null;
        }
    }

    private int parseInt(String value, int fallback) {
        if (value == null) return fallback;
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return fallback; }
    }
}