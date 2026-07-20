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
 * 写入时清空 user-api 的 planCatalog / plans Caffeine 缓存。
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
        entity.setInviterReward(req.getInviterReward());
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
        // 提示：user-api 侧的 planCatalog / plans 缓存在另一进程，需重启或等待 TTL 过期。
        // 这里只能清本进程缓存；跨进程失效依赖 Caffeine 5–10 分钟 TTL 或下次重启。
        return PlanVO.from(entity);
    }
}