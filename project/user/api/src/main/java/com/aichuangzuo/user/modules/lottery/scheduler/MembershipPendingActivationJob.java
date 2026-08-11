package com.aichuangzuo.user.modules.lottery.scheduler;

import com.aichuangzuo.user.modules.lottery.entity.UserMembershipPending;
import com.aichuangzuo.user.modules.lottery.mapper.UserMembershipPendingMapper;
import com.aichuangzuo.user.modules.membership.service.MembershipService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipPendingActivationJob {

    private final UserMembershipPendingMapper pendingMapper;
    private final MembershipService membershipService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void activatePendingMemberships() {
        LocalDate today = LocalDate.now();
        List<UserMembershipPending> pendings = pendingMapper.selectList(
                new LambdaQueryWrapper<UserMembershipPending>()
                        .eq(UserMembershipPending::getStatus, "pending")
                        .le(UserMembershipPending::getPlannedStartAt, today)
                        .orderByAsc(UserMembershipPending::getCreatedAt));

        for (UserMembershipPending pending : pendings) {
            try {
                membershipService.extendMembership(pending.getUserId(), pending.getPlanKey(), pending.getDays());
                pending.setStatus("activated");
                pending.setActivatedAt(LocalDateTime.now());
                pendingMapper.updateById(pending);
                log.info("激活待生效会员 pendingId={}, userId={}, planKey={}, days={}",
                        pending.getId(), pending.getUserId(), pending.getPlanKey(), pending.getDays());
            } catch (Exception e) {
                log.error("激活待生效会员失败 pendingId={}", pending.getId(), e);
            }
        }
    }
}
