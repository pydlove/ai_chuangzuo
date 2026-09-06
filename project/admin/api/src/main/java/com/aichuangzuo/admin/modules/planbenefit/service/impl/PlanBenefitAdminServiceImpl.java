package com.aichuangzuo.admin.modules.planbenefit.service.impl;

import com.aichuangzuo.admin.modules.plan.entity.Plan;
import com.aichuangzuo.admin.modules.plan.mapper.PlanMapper;
import com.aichuangzuo.admin.modules.planbenefit.dto.request.PlanBenefitUpsertRequest;
import com.aichuangzuo.admin.modules.planbenefit.entity.PlanBenefit;
import com.aichuangzuo.admin.modules.planbenefit.mapper.PlanBenefitMapper;
import com.aichuangzuo.admin.modules.planbenefit.service.PlanBenefitAdminService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理端 u_plan_benefit 维护：拉全表 + 单条 upsert（按 planKey+benefitCode 唯一）。
 * user-api 侧的 planCatalog / plans / planBenefits 缓存在另一进程，需等 5–10 分钟 TTL 过期或重启。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanBenefitAdminServiceImpl implements PlanBenefitAdminService {

    /** AI 文章生成额度编码，变更时需同步 u_plan 文章文案。 */
    private static final String BENEFIT_AI_ARTICLE_QUOTA = "ai_article_quota";

    private final PlanBenefitMapper planBenefitMapper;
    private final PlanMapper planMapper;

    @Override
    public List<PlanBenefit> list() {
        return planBenefitMapper.selectList(
                new LambdaQueryWrapper<PlanBenefit>()
                        .orderByAsc(PlanBenefit::getPlanKey, PlanBenefit::getBenefitCode));
    }

    @Override
    @CacheEvict(cacheNames = {"adminPlanList"}, allEntries = true)
    public PlanBenefit upsert(PlanBenefitUpsertRequest request, Long adminUserId) {
        PlanBenefit existing = planBenefitMapper.selectOne(new LambdaQueryWrapper<PlanBenefit>()
                .eq(PlanBenefit::getPlanKey, request.getPlanKey())
                .eq(PlanBenefit::getBenefitCode, request.getBenefitCode()));
        PlanBenefit entity = existing == null ? new PlanBenefit() : existing;
        entity.setPlanKey(request.getPlanKey());
        entity.setBenefitCode(request.getBenefitCode());
        entity.setBenefitValue(request.getBenefitValue());

        if (existing == null) {
            planBenefitMapper.insert(entity);
            log.info("新增套餐权益 planKey={}, code={}, value={}, adminUserId={}",
                    request.getPlanKey(), request.getBenefitCode(), request.getBenefitValue(), adminUserId);
        } else {
            planBenefitMapper.updateById(entity);
            log.info("更新套餐权益 planKey={}, code={}, value={}, adminUserId={}",
                    request.getPlanKey(), request.getBenefitCode(), request.getBenefitValue(), adminUserId);
        }

        if (BENEFIT_AI_ARTICLE_QUOTA.equals(request.getBenefitCode())) {
            syncPlanArticleText(request.getPlanKey(), request.getBenefitValue());
        }
        return entity;
    }

    /**
     * 当 ai_article_quota 变更时，同步回写 u_plan 的 articles_* 展示文案，
     * 避免定价卡片与真实额度不一致。
     */
    private void syncPlanArticleText(String planKey, String benefitValue) {
        int quota = parseInt(benefitValue, -1);
        if (quota < 0) {
            log.warn("ai_article_quota 非数字，跳过同步 u_plan.articles_* planKey={}, value={}",
                    planKey, benefitValue);
            return;
        }
        Plan plan = planMapper.selectOne(new LambdaQueryWrapper<Plan>()
                .eq(Plan::getPlanKey, planKey));
        if (plan == null) {
            log.warn("未找到套餐 planKey={}，无法同步文章文案", planKey);
            return;
        }
        plan.setArticlesMonthly(quota > 0 ? quota + " 篇 AI 文章/月" : null);
        plan.setArticlesQuarter(quota > 0 ? (quota * 3) + " 篇 AI 文章/季" : null);
        plan.setArticlesYear(quota > 0 ? (quota * 12) + " 篇 AI 文章/年" : null);
        planMapper.updateById(plan);
        log.info("已同步 u_plan 文章文案 planKey={}, quota={}", planKey, quota);
    }

    private int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}