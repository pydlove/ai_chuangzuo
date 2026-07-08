package com.aichuangzuo.user.modules.membership.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.entity.UserInviteRelation;
import com.aichuangzuo.user.modules.auth.mapper.UserInviteRelationMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord;
import com.aichuangzuo.user.modules.leaderboard.mapper.UserCoinRecordMapper;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import com.aichuangzuo.user.modules.membership.dto.request.SubscribeRequest;
import com.aichuangzuo.user.modules.membership.entity.Order;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.enums.MembershipCycle;
import com.aichuangzuo.user.modules.membership.enums.MembershipErrorCode;
import com.aichuangzuo.user.modules.membership.enums.MembershipPlan;
import com.aichuangzuo.user.modules.membership.mapper.OrderMapper;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import com.aichuangzuo.user.modules.membership.service.MembershipService;
import com.aichuangzuo.user.modules.membership.vo.MembershipStatusVO;
import com.aichuangzuo.user.modules.membership.vo.SubscribeResultVO;
import com.aichuangzuo.user.modules.message.enums.MessageSubType;
import com.aichuangzuo.user.modules.message.service.MessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 会员服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private static final String TEST_PAY_CODE = "123456";
    private static final String ORDER_NO_PREFIX = "SUB";
    private static final String COIN_BIZ_TYPE_INVITE_REWARD = "invite_reward";
    private static final int EFFECTIVE_STATUS = 1;

    private final OrderMapper orderMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final UserInviteRelationMapper userInviteRelationMapper;
    private final UserCoinRecordMapper userCoinRecordMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final CoinRecordService coinRecordService;

    @Override
    @Transactional
    public SubscribeResultVO subscribe(Long userId, SubscribeRequest request) {
        validatePayCode(request.getPayCode());

        MembershipPlan plan = MembershipPlan.of(request.getPlanKey());
        if (plan == null) {
            throw new BusinessException(MembershipErrorCode.INVALID_PLAN_KEY);
        }

        MembershipCycle cycle = MembershipCycle.of(request.getCycle());
        if (cycle == null) {
            throw new BusinessException(MembershipErrorCode.INVALID_CYCLE);
        }

        Order order = createPaidOrder(userId, plan, cycle, request.getAmount());
        UserMembership membership = activateOrExtendMembership(userId, plan, cycle);

        sendSubscriptionNotification(userId, plan, membership);
        boolean rewarded = rewardInviter(userId, plan, order);

        log.info("会员订阅成功 userId={}, orderNo={}, level={}, days={}",
                userId, order.getOrderNo(), plan.getKey(), cycle.getDays());

        SubscribeResultVO vo = new SubscribeResultVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setLevel(plan.getKey());
        vo.setDays(cycle.getDays());
        vo.setExpiresAt(membership.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE));
        vo.setInviterRewarded(rewarded);
        vo.setRewardAmount(rewarded ? plan.getInviterReward() : BigDecimal.ZERO);
        return vo;
    }

    @Override
    public MembershipStatusVO getMyMembership(Long userId) {
        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        MembershipStatusVO vo = new MembershipStatusVO();
        if (membership == null || membership.getExpiresAt().isBefore(LocalDate.now())) {
            vo.setHasMembership(false);
            return vo;
        }

        MembershipPlan plan = MembershipPlan.of(membership.getLevel());
        vo.setHasMembership(true);
        vo.setLevel(membership.getLevel());
        vo.setLevelName(plan == null ? membership.getLevel() : plan.getDisplayName());
        vo.setExpiresAt(membership.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE));
        return vo;
    }

    private void validatePayCode(String payCode) {
        if (!TEST_PAY_CODE.equals(payCode)) {
            throw new BusinessException(MembershipErrorCode.INVALID_PAY_CODE);
        }
    }

    private Order createPaidOrder(Long userId, MembershipPlan plan, MembershipCycle cycle, BigDecimal amount) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setPlanKey(plan.getKey());
        order.setCycle(cycle.getCode());
        order.setAmount(amount);
        order.setStatus(1);
        order.setPaidAt(LocalDateTime.now());
        order.setTenantId(0L);
        orderMapper.insert(order);
        return order;
    }

    private String generateOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String random = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        return ORDER_NO_PREFIX + date + random;
    }

    private UserMembership activateOrExtendMembership(Long userId, MembershipPlan plan, MembershipCycle cycle) {
        UserMembership membership = userMembershipMapper.selectByUserId(userId);
        LocalDate today = LocalDate.now();
        LocalDate baseDate = today;
        if (membership != null && membership.getExpiresAt().isAfter(today.minusDays(1))) {
            baseDate = membership.getExpiresAt();
        }
        LocalDate newExpiresAt = baseDate.plusDays(cycle.getDays());

        if (membership == null) {
            membership = new UserMembership();
            membership.setUserId(userId);
            membership.setLevel(plan.getKey());
            membership.setStartedAt(today);
            membership.setExpiresAt(newExpiresAt);
            membership.setTenantId(0L);
            userMembershipMapper.insert(membership);
        } else {
            membership.setLevel(plan.getKey());
            membership.setStartedAt(today);
            membership.setExpiresAt(newExpiresAt);
            userMembershipMapper.updateById(membership);
        }
        return membership;
    }

    private void sendSubscriptionNotification(Long userId, MembershipPlan plan, UserMembership membership) {
        String levelName = plan.getDisplayName();
        String expiresAt = membership.getExpiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String summary = String.format("您已成功开通 %s，有效期至 %s", levelName, expiresAt);
        String content = String.format(
                "亲爱的用户：\n\n您的 %s 会员已成功开通，有效期至 %s。\n\n感谢您对爱创作的支持！",
                levelName, expiresAt);

        messageService.pushPersonal(
                userId,
                "membership",
                "订阅成功",
                summary,
                null,
                content,
                MessageSubType.SUBSCRIBED.getCode());
    }

    private boolean rewardInviter(Long userId, MembershipPlan plan, Order order) {
        UserInviteRelation relation = userInviteRelationMapper.selectByInviteeId(userId);
        if (relation == null || !Integer.valueOf(EFFECTIVE_STATUS).equals(relation.getEffectiveStatus())) {
            return false;
        }

        Long inviterId = relation.getInviterId();
        if (alreadyRewarded(order.getId())) {
            log.warn("邀请奖励已发放，跳过 userId={}, orderId={}", userId, order.getId());
            return false;
        }

        User invitee = userMapper.selectById(userId);
        String inviteeName = invitee == null ? "好友" : (invitee.getNickname() == null ? "好友" : invitee.getNickname());
        String remark = String.format("%s 订阅 %s，邀请奖励", inviteeName, plan.getDisplayName());

        coinRecordService.grant(inviterId, COIN_BIZ_TYPE_INVITE_REWARD, plan.getInviterReward(), order.getId().toString(), remark);

        String summary = String.format("好友 %s 订阅 %s，您获得 %s 创作币", inviteeName, plan.getDisplayName(), plan.getInviterReward().toPlainString());
        String content = String.format(
                "恭喜您！\n\n您邀请的好友 %s 成功订阅 %s，系统已向您发放 %s 创作币奖励。\n\n感谢您的分享！",
                inviteeName, plan.getDisplayName(), plan.getInviterReward().toPlainString());

        messageService.pushPersonal(inviterId, "reward", "邀请奖励到账", summary, null, content, null);
        return true;
    }

    private boolean alreadyRewarded(Long orderId) {
        Long count = userCoinRecordMapper.selectCount(
                new LambdaQueryWrapper<UserCoinRecord>()
                        .eq(UserCoinRecord::getRefId, orderId.toString())
                        .eq(UserCoinRecord::getBizType, COIN_BIZ_TYPE_INVITE_REWARD)
        );
        return count != null && count > 0;
    }
}
