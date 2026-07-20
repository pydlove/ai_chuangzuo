package com.aichuangzuo.admin.modules.planbenefit.service.impl;

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

    private final PlanBenefitMapper planBenefitMapper;

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
        return entity;
    }
}