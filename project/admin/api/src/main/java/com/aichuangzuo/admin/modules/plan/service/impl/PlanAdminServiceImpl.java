package com.aichuangzuo.admin.modules.plan.service.impl;

import com.aichuangzuo.admin.modules.plan.dto.request.PlanUpsertRequest;
import com.aichuangzuo.admin.modules.plan.entity.Plan;
import com.aichuangzuo.admin.modules.plan.mapper.PlanMapper;
import com.aichuangzuo.admin.modules.plan.service.PlanAdminService;
import com.aichuangzuo.admin.modules.plan.vo.PlanVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端 u_plan 维护：列表查询 + upsert（按 planKey 唯一）。
 * 写入后用户端 planCatalog 缓存通过 updated_at 版本 key 即时失效（见 user-api PlanCacheVersionKeyProvider）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanAdminServiceImpl implements PlanAdminService {

    private final PlanMapper planMapper;

    @Override
    @Cacheable(cacheNames = "adminPlanList", key = "'all'")
    public List<PlanVO> list() {
        return planMapper.selectList(new LambdaQueryWrapper<Plan>()
                .orderByAsc(Plan::getSortOrder))
                .stream().map(PlanVO::from).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = {"adminPlanList"}, allEntries = true)
    public PlanVO upsert(PlanUpsertRequest req, Long adminUserId) {
        Plan existing = planMapper.selectOne(
                new LambdaQueryWrapper<Plan>().eq(Plan::getPlanKey, req.getPlanKey()));
        Plan entity = existing == null ? new Plan() : existing;
        entity.setPlanKey(req.getPlanKey());
        entity.setDisplayName(req.getDisplayName());
        entity.setSortOrder(req.getSortOrder());
        entity.setRecommended(req.getRecommended());
        entity.setPriceMonthly(req.getPriceMonthly());
        entity.setPriceQuarter(req.getPriceQuarter());
        entity.setPriceYear(req.getPriceYear());
        entity.setOriginalMonthly(req.getOriginalMonthly());
        entity.setOriginalQuarter(req.getOriginalQuarter());
        entity.setOriginalYear(req.getOriginalYear());
        entity.setArticlesMonthly(req.getArticlesMonthly());
        entity.setArticlesQuarter(req.getArticlesQuarter());
        entity.setArticlesYear(req.getArticlesYear());
        entity.setSavingsYear(req.getSavingsYear());
        entity.setStatus(req.getStatus());
        entity.setTenantId(0L);
        entity.setUpdatedBy(adminUserId);

        if (existing == null) {
            entity.setCreatedBy(adminUserId);
            planMapper.insert(entity);
            log.info("新增套餐 planKey={}, adminUserId={}", entity.getPlanKey(), adminUserId);
        } else {
            planMapper.updateById(entity);
            log.info("更新套餐 planKey={}, adminUserId={}", entity.getPlanKey(), adminUserId);
        }
        // 用户端 planCatalog 缓存 key 以 u_plan 最大 updated_at 为版本，此处保存后版本即变化，
        // 用户端下一次请求自动落到新 key，无需重启或等待 TTL。
        return PlanVO.from(entity);
    }
}