package com.aichuangzuo.user.modules.user.service.impl;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.entity.UserInviteRelation;
import com.aichuangzuo.user.modules.auth.mapper.UserInviteRelationMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.leaderboard.entity.CoinDirection;
import com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord;
import com.aichuangzuo.user.modules.leaderboard.mapper.UserCoinRecordMapper;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import com.aichuangzuo.user.modules.membership.entity.Order;
import com.aichuangzuo.user.modules.membership.mapper.OrderMapper;
import com.aichuangzuo.user.modules.membership.service.MembershipService;
import com.aichuangzuo.user.modules.message.enums.MessageSubType;
import com.aichuangzuo.user.modules.message.service.MessageService;
import com.aichuangzuo.user.modules.user.service.InviteRewardService;
import com.aichuangzuo.user.modules.user.vo.InviteFriendVO;
import com.aichuangzuo.user.modules.user.vo.InviteStatsVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 邀请奖励服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InviteRewardServiceImpl implements InviteRewardService {

    private static final String COIN_BIZ_TYPE_REGISTER_REWARD = "invite_register_reward";
    private static final String COIN_BIZ_TYPE_INVITE_REWARD = "invite_reward";
    private static final String MEMBERSHIP_LEVEL_PRO = "pro";
    private static final BigDecimal NEW_USER_COIN_BONUS = new BigDecimal("5");
    private static final int EFFECTIVE_STATUS = 1;

    private final UserMapper userMapper;
    private final UserInviteRelationMapper userInviteRelationMapper;
    private final UserCoinRecordMapper userCoinRecordMapper;
    private final OrderMapper orderMapper;
    private final CoinRecordService coinRecordService;
    private final MembershipService membershipService;
    private final MessageService messageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rewardAfterRegister(User invitee, String inviteCode) {
        User inviter = userMapper.selectByInviteCode(inviteCode);
        if (inviter == null) {
            throw new BusinessException(UserAuthErrorCode.INVITE_CODE_INVALID);
        }

        UserInviteRelation relation = new UserInviteRelation();
        relation.setInviterId(inviter.getId());
        relation.setInviteeId(invitee.getId());
        relation.setInviteCode(inviteCode);
        relation.setSourceType(2);
        relation.setEffectiveStatus(EFFECTIVE_STATUS);
        userInviteRelationMapper.insert(relation);

        grantRegisterReward(invitee, inviter);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rewardAfterBinding(User invitee, User inviter) {
        grantRegisterReward(invitee, inviter);
    }

    private void grantRegisterReward(User invitee, User inviter) {
        coinRecordService.grant(invitee.getId(), COIN_BIZ_TYPE_REGISTER_REWARD, NEW_USER_COIN_BONUS,
                null, "邀请注册奖励");
        log.info("被邀请人 {} 获得 {} 创作币，邀请人 {}", invitee.getEmail(), NEW_USER_COIN_BONUS, inviter.getEmail());

        long count = userInviteRelationMapper.selectCount(
                new LambdaQueryWrapper<UserInviteRelation>()
                        .eq(UserInviteRelation::getInviterId, inviter.getId())
                        .eq(UserInviteRelation::getEffectiveStatus, EFFECTIVE_STATUS)
        );
        long rewardDays = calculateMembershipRewardDays(count);
        if (rewardDays > 0) {
            membershipService.extendMembership(inviter.getId(), MEMBERSHIP_LEVEL_PRO, rewardDays);
            sendMembershipRewardMessage(inviter, count, rewardDays);
            log.info("邀请人 {} 累计有效邀请达到阶梯，获得 {} 天专业版会员", inviter.getEmail(), rewardDays);
        }
    }

    private void sendMembershipRewardMessage(User inviter, long count, long rewardDays) {
        String summary = String.format("恭喜！您累计邀请 %d 位好友，获得 %d 天专业版会员", count, rewardDays);
        String content = String.format(
                "亲爱的用户：\n\n恭喜您累计邀请 %d 位好友加入爱创作，系统已为您发放 %d 天专业版会员奖励。\n\n"
                        + "会员权益已自动生效，您可前往「我的」页面查看会员有效期。\n\n感谢您的分享！",
                count, rewardDays);
        messageService.pushPersonal(inviter.getId(), "membership", "邀请会员奖励到账",
                summary, null, content, MessageSubType.INVITE_REWARD.getCode());
    }

    /**
     * 根据邀请人当前累计有效邀请数，计算本次应额外奖励的会员天数。
     *
     * <p>阶梯规则：第 3 人 +3 天，第 5 人 +5 天，第 6 人起每多 1 人 +2 天。
     *
     * @param count 累计有效邀请数
     * @return 本次应奖励的天数；不触发阶梯时返回 0
     */
    private long calculateMembershipRewardDays(long count) {
        if (count == 3) {
            return 3;
        }
        if (count == 5) {
            return 5;
        }
        if (count > 5) {
            return 2;
        }
        return 0;
    }

    @Override
    public InviteStatsVO getInviteStats(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(UserAuthErrorCode.USER_NOT_FOUND);
        }

        InviteStatsVO vo = new InviteStatsVO();
        vo.setInviteCode(user.getInviteCode());
        vo.setCoinBalance(user.getCoinBalance() == null ? BigDecimal.ZERO : user.getCoinBalance());

        List<UserInviteRelation> relations = userInviteRelationMapper.selectList(
                new LambdaQueryWrapper<UserInviteRelation>()
                        .eq(UserInviteRelation::getInviterId, userId)
                        .eq(UserInviteRelation::getEffectiveStatus, EFFECTIVE_STATUS)
                        .orderByDesc(UserInviteRelation::getCreatedAt)
        );
        vo.setInvitedCount(relations.size());
        vo.setMembershipDaysEarned(calculateTotalMembershipDays(relations.size()));

        List<UserCoinRecord> rewardRecords = userCoinRecordMapper.selectList(
                new LambdaQueryWrapper<UserCoinRecord>()
                        .eq(UserCoinRecord::getUserId, userId)
                        .eq(UserCoinRecord::getBizType, COIN_BIZ_TYPE_INVITE_REWARD)
                        .eq(UserCoinRecord::getDirection, CoinDirection.INCOME.getCode())
        );
        BigDecimal coinEarned = rewardRecords.stream()
                .map(r -> r.getAmount() == null ? BigDecimal.ZERO : r.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setCoinEarned(coinEarned);

        vo.setFriends(buildFriends(relations, rewardRecords));
        return vo;
    }

    /**
     * 根据累计有效邀请数计算累计获得的会员天数。
     *
     * <p>规则：3 人 +3 天，5 人 +5 天，>5 人后每多 1 人 +2 天。
     * 示例：6 人 = 3 + 5 + 2 = 10 天。
     *
     * @param count 累计有效邀请数
     * @return 累计会员天数
     */
    private int calculateTotalMembershipDays(int count) {
        int days = 0;
        if (count >= 3) {
            days += 3;
        }
        if (count >= 5) {
            days += 5;
        }
        if (count > 5) {
            days += (count - 5) * 2;
        }
        return days;
    }

    private List<InviteFriendVO> buildFriends(List<UserInviteRelation> relations,
                                                List<UserCoinRecord> rewardRecords) {
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> inviteeIds = relations.stream()
                .map(UserInviteRelation::getInviteeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> inviteeMap = userMapper.selectBatchIds(inviteeIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Map<Long, List<Order>> ordersByUser = orderMapper.selectList(
                        new LambdaQueryWrapper<Order>()
                                .in(Order::getUserId, inviteeIds)
                                .eq(Order::getStatus, 1))
                .stream()
                .collect(Collectors.groupingBy(Order::getUserId));

        Set<String> orderRefIds = ordersByUser.values().stream()
                .flatMap(List::stream)
                .map(o -> String.valueOf(o.getId()))
                .collect(Collectors.toSet());

        Map<Long, BigDecimal> commissionByInvitee = rewardRecords.stream()
                .filter(r -> r.getRefId() != null && orderRefIds.contains(r.getRefId()))
                .flatMap(r -> ordersByUser.values().stream()
                        .flatMap(List::stream)
                        .filter(o -> String.valueOf(o.getId()).equals(r.getRefId()))
                        .map(o -> Map.entry(o.getUserId(), r.getAmount() == null ? BigDecimal.ZERO : r.getAmount())))
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.reducing(BigDecimal.ZERO, Map.Entry::getValue, BigDecimal::add)));

        return relations.stream().map(relation -> {
            Long inviteeId = relation.getInviteeId();
            User invitee = inviteeMap.get(inviteeId);
            InviteFriendVO friend = new InviteFriendVO();
            friend.setEmail(invitee == null ? "" : invitee.getEmail());
            friend.setNickname(invitee == null ? null : invitee.getNickname());
            BigDecimal commission = commissionByInvitee.getOrDefault(inviteeId, BigDecimal.ZERO);
            friend.setCommission(commission);
            friend.setStatus(commission.compareTo(BigDecimal.ZERO) > 0 ? "purchased" : "registered");
            return friend;
        }).collect(Collectors.toList());
    }
}
