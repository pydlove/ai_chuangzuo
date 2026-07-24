package com.aichuangzuo.user.modules.membership.service.impl;

import com.aichuangzuo.user.modules.membership.entity.Plan;
import com.aichuangzuo.user.modules.membership.enums.MembershipPlan;
import com.aichuangzuo.user.modules.membership.mapper.PlanMapper;
import com.aichuangzuo.user.modules.membership.service.PlanLookupService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 套餐元数据查询实现。DB 未命中时回退到枚举默认配置。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanLookupServiceImpl implements PlanLookupService {

    private final PlanMapper planMapper;

    @Override
    @Cacheable(cacheNames = "plans", key = "#planKey")
    public Plan findActive(String planKey) {
        if (planKey == null) return null;
        return planMapper.selectOne(new LambdaQueryWrapper<Plan>()
                .eq(Plan::getPlanKey, planKey)
                .eq(Plan::getStatus, 1));
    }

    @Override
    public String getDisplayName(String planKey) {
        Plan plan = findActive(planKey);
        if (plan != null && plan.getDisplayName() != null) {
            return plan.getDisplayName();
        }
        MembershipPlan fallback = MembershipPlan.of(planKey);
        return fallback == null ? planKey : fallback.getDisplayName();
    }
}