package com.aichuangzuo.user.modules.generation.service;

import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.generation.mapper.UserMembershipMirrorMapper;
import com.aichuangzuo.user.modules.membership.enums.MembershipPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 会员权益解析：把 u_user.membership_plan / membership_expire_at 映射到生成场景的两个值：
 * <ul>
 *   <li>ratePerMinute：每分钟可提交的任务数（从 u_plan_benefit 读取，默认 3/5/8）</li>
 *   <li>retentionDays：任务保留天数（null=永久）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class GenerationBenefitResolver {

    private static final String GENERATION_RATE_LIMIT_CODE = "generation_rate_limit";

    private final UserMembershipMirrorMapper userMapper;
    private final BenefitService benefitService;

    public int ratePerMinute(Long userId) {
        MembershipPlan plan = currentPlan(userId);
        String defaultValue = defaultRate(plan);
        String configured = benefitService.getPlanBenefitValue(userId, GENERATION_RATE_LIMIT_CODE, defaultValue);
        return parsePositiveInt(configured, parsePositiveInt(defaultValue, 3));
    }

    public Integer retentionDays(Long userId) {
        MembershipPlan plan = currentPlan(userId);
        if (plan == MembershipPlan.FLAGSHIP || plan == MembershipPlan.PRO) return null;
        return 30;
    }

    private MembershipPlan currentPlan(Long userId) {
        UserMembershipMirrorMapper.MembershipMirror m = userMapper.selectMembership(userId);
        if (m == null) return null;
        LocalDateTime expireAt = m.getExpireAt();
        if (expireAt == null || expireAt.isBefore(LocalDateTime.now())) return null;
        return MembershipPlan.of(m.getPlanKey());
    }

    private static String defaultRate(MembershipPlan plan) {
        if (plan == MembershipPlan.FLAGSHIP) return "8";
        if (plan == MembershipPlan.PRO) return "5";
        return "3";
    }

    private static int parsePositiveInt(String value, int fallback) {
        if (value == null) return fallback;
        try {
            int i = Integer.parseInt(value.trim());
            return i > 0 ? i : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
