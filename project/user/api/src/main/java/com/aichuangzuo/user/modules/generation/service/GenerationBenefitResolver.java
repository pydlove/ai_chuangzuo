package com.aichuangzuo.user.modules.generation.service;

import com.aichuangzuo.user.modules.generation.mapper.UserMembershipMirrorMapper;
import com.aichuangzuo.user.modules.membership.enums.MembershipPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 会员权益解析：把 u_user.membership_plan / membership_expire_at 映射到生成场景的两个值：
 * <ul>
 *   <li>ratePerMinute：每分钟可提交的任务数</li>
 *   <li>retentionDays：任务保留天数（null=永久）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class GenerationBenefitResolver {

    private final UserMembershipMirrorMapper userMapper;

    public int ratePerMinute(Long userId) {
        MembershipPlan plan = currentPlan(userId);
        if (plan == MembershipPlan.FLAGSHIP) return 10;
        if (plan == MembershipPlan.PRO) return 5;
        return 1;
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
}
