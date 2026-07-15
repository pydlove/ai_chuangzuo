package com.aichuangzuo.user.modules.benefit.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.benefit.entity.Benefit;
import com.aichuangzuo.user.modules.benefit.entity.BenefitUsage;
import com.aichuangzuo.user.modules.benefit.entity.PlanBenefit;
import com.aichuangzuo.user.modules.benefit.enums.BenefitErrorCode;
import com.aichuangzuo.user.modules.benefit.mapper.BenefitMapper;
import com.aichuangzuo.user.modules.benefit.mapper.BenefitUsageMapper;
import com.aichuangzuo.user.modules.benefit.mapper.PlanBenefitMapper;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
import com.aichuangzuo.user.modules.benefit.vo.UserBenefitVO;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.enums.MembershipPlan;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 会员权益服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BenefitServiceImpl implements BenefitService {

    private static final String FREE_PLAN_KEY = "free";
    private static final String FREE_PLAN_NAME = "免费版";
    private static final String TYPE_BOOLEAN = "boolean";
    private static final String TYPE_QUOTA = "quota";

    private final BenefitMapper benefitMapper;
    private final PlanBenefitMapper planBenefitMapper;
    private final BenefitUsageMapper benefitUsageMapper;
    private final UserMembershipMapper userMembershipMapper;

    @Override
    public UserBenefitVO getMyBenefits(Long userId) {
        String planKey = currentPlanKey(userId);
        UserBenefitVO vo = new UserBenefitVO();
        vo.setPlanKey(planKey);

        if (FREE_PLAN_KEY.equals(planKey)) {
            vo.setPlanName(FREE_PLAN_NAME);
            vo.setBenefits(new ArrayList<>());
            return vo;
        }

        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        MembershipPlan plan = MembershipPlan.of(planKey);
        vo.setPlanName(plan == null ? planKey : plan.getDisplayName());
        vo.setExpiresAt(membership.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE));

        Map<String, Benefit> benefitMap = loadActiveBenefitMap();
        String period = currentPeriod();
        List<UserBenefitVO.BenefitItem> items = new ArrayList<>();
        for (PlanBenefit pb : listPlanBenefits(planKey)) {
            Benefit benefit = benefitMap.get(pb.getBenefitCode());
            if (benefit == null) {
                continue;
            }
            UserBenefitVO.BenefitItem item = new UserBenefitVO.BenefitItem();
            item.setCode(benefit.getCode());
            item.setName(benefit.getName());
            item.setType(benefit.getType());
            item.setValue(pb.getBenefitValue());
            if (TYPE_QUOTA.equals(benefit.getType())) {
                int limit = parseInt(pb.getBenefitValue(), 0);
                int used = currentUsed(userId, benefit.getCode(), period);
                item.setUsed(used);
                item.setRemaining(Math.max(limit - used, 0));
            }
            items.add(item);
        }
        vo.setBenefits(items);
        return vo;
    }

    @Override
    public BenefitCheckVO check(Long userId, String code) {
        Benefit benefit = requireBenefit(code);
        String planKey = currentPlanKey(userId);

        BenefitCheckVO vo = new BenefitCheckVO();
        vo.setCode(code);
        vo.setType(benefit.getType());

        PlanBenefit planBenefit = findPlanBenefit(planKey, code);
        if (planBenefit == null) {
            deny(vo, null, BenefitErrorCode.BENEFIT_NOT_SUPPORTED.getMessage());
            return vo;
        }
        vo.setValue(planBenefit.getBenefitValue());

        if (TYPE_BOOLEAN.equals(benefit.getType())) {
            if (Boolean.parseBoolean(planBenefit.getBenefitValue())) {
                vo.setAllowed(true);
            } else {
                deny(vo, planBenefit.getBenefitValue(), "当前套餐不支持此功能，请升级");
            }
            return vo;
        }

        if (TYPE_QUOTA.equals(benefit.getType())) {
            int limit = parseInt(planBenefit.getBenefitValue(), 0);
            int used = currentUsed(userId, code, currentPeriod());
            vo.setUsed(used);
            vo.setRemaining(Math.max(limit - used, 0));
            if (used < limit) {
                vo.setAllowed(true);
            } else {
                deny(vo, planBenefit.getBenefitValue(), BenefitErrorCode.QUOTA_EXHAUSTED.getMessage());
            }
            return vo;
        }

        // tier 类型：放行，值交给业务逻辑自行判断
        vo.setAllowed(true);
        return vo;
    }

    @Override
    public BenefitCheckVO consume(Long userId, String code) {
        Benefit benefit = requireBenefit(code);
        if (!TYPE_QUOTA.equals(benefit.getType())) {
            throw new BusinessException(BenefitErrorCode.NOT_QUOTA_BENEFIT);
        }

        String planKey = currentPlanKey(userId);
        PlanBenefit planBenefit = findPlanBenefit(planKey, code);
        if (planBenefit == null) {
            throw new BusinessException(BenefitErrorCode.BENEFIT_NOT_SUPPORTED);
        }

        int limit = parseInt(planBenefit.getBenefitValue(), 0);
        String period = currentPeriod();

        int updated = benefitUsageMapper.incrementIfBelowLimit(userId, code, period, limit);
        if (updated == 0) {
            BenefitUsage existing = benefitUsageMapper.selectByUserAndCodeAndPeriod(userId, code, period);
            if (existing != null || limit < 1) {
                throw new BusinessException(BenefitErrorCode.QUOTA_EXHAUSTED);
            }
            // 本期首次消费：插入用量记录；并发插入冲突时退回到原子 +1
            BenefitUsage usage = new BenefitUsage();
            usage.setUserId(userId);
            usage.setBenefitCode(code);
            usage.setPeriod(period);
            usage.setUsedCount(1);
            usage.setTenantId(0L);
            try {
                benefitUsageMapper.insert(usage);
            } catch (DuplicateKeyException e) {
                if (benefitUsageMapper.incrementIfBelowLimit(userId, code, period, limit) == 0) {
                    throw new BusinessException(BenefitErrorCode.QUOTA_EXHAUSTED);
                }
            }
        }

        BenefitUsage current = benefitUsageMapper.selectByUserAndCodeAndPeriod(userId, code, period);
        int used = current == null ? 0 : current.getUsedCount();

        BenefitCheckVO vo = new BenefitCheckVO();
        vo.setAllowed(true);
        vo.setCode(code);
        vo.setType(benefit.getType());
        vo.setValue(planBenefit.getBenefitValue());
        vo.setUsed(used);
        vo.setRemaining(Math.max(limit - used, 0));
        return vo;
    }

    @Override
    public void refund(Long userId, String code) {
        int updated = benefitUsageMapper.decrementIfAboveZero(userId, code, currentPeriod());
        log.info("权益额度退回 userId={}, code={}, updated={}", userId, code, updated);
    }

    /**
     * 查询套餐的全部权益值（带缓存，key 为 planKey）。
     */
    @Cacheable(cacheNames = "planBenefits", key = "#planKey")
    public List<PlanBenefit> listPlanBenefits(String planKey) {
        return planBenefitMapper.selectByPlanKey(planKey);
    }

    // ── private helpers ──

    /**
     * 当前有效套餐 key；无会员或已过期返回 free。
     */
    private String currentPlanKey(Long userId) {
        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        if (membership == null || membership.getExpiresAt().isBefore(LocalDate.now())) {
            return FREE_PLAN_KEY;
        }
        return membership.getLevel();
    }

    private Benefit requireBenefit(String code) {
        Benefit benefit = benefitMapper.selectByCode(code);
        if (benefit == null) {
            throw new BusinessException(BenefitErrorCode.BENEFIT_NOT_FOUND);
        }
        return benefit;
    }

    private PlanBenefit findPlanBenefit(String planKey, String code) {
        if (FREE_PLAN_KEY.equals(planKey)) {
            return null;
        }
        return listPlanBenefits(planKey).stream()
                .filter(pb -> code.equals(pb.getBenefitCode()))
                .findFirst()
                .orElse(null);
    }

    private Map<String, Benefit> loadActiveBenefitMap() {
        return benefitMapper.selectList(null).stream()
                .filter(b -> Integer.valueOf(1).equals(b.getStatus()))
                .collect(Collectors.toMap(Benefit::getCode, Function.identity()));
    }

    private int currentUsed(Long userId, String code, String period) {
        BenefitUsage usage = benefitUsageMapper.selectByUserAndCodeAndPeriod(userId, code, period);
        return usage == null ? 0 : usage.getUsedCount();
    }

    private String currentPeriod() {
        return YearMonth.now().toString();
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void deny(BenefitCheckVO vo, String value, String message) {
        vo.setAllowed(false);
        vo.setValue(value);
        vo.setMessage(message);
    }
}
