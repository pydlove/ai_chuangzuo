package com.aichuangzuo.user.modules.user.service.impl;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.entity.UserInviteRelation;
import com.aichuangzuo.user.modules.auth.mapper.UserInviteRelationMapper;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.earnings.enums.EarningsType;
import com.aichuangzuo.user.modules.earnings.service.EarningsService;
import com.aichuangzuo.user.modules.leaderboard.entity.CoinDirection;
import com.aichuangzuo.user.modules.leaderboard.entity.UserCoinRecord;
import com.aichuangzuo.user.modules.leaderboard.mapper.UserCoinRecordMapper;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import com.aichuangzuo.user.modules.lottery.entity.LotteryCampaign;
import com.aichuangzuo.user.modules.lottery.mapper.LotteryCampaignMapper;
import com.aichuangzuo.user.modules.lottery.service.LotteryChanceService;
import com.aichuangzuo.user.modules.membership.entity.Order;
import com.aichuangzuo.user.modules.membership.mapper.OrderMapper;
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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
    private static final String COIN_BIZ_TYPE_LADDER_REWARD = "invite_ladder_reward";
    private static final String COIN_BIZ_TYPE_INVITE_REWARD = "invite_reward";
    private static final BigDecimal NEW_USER_COIN_BONUS = new BigDecimal("50");
    private static final int EFFECTIVE_STATUS = 1;

    private final UserMapper userMapper;
    private final UserInviteRelationMapper userInviteRelationMapper;
    private final UserCoinRecordMapper userCoinRecordMapper;
    private final OrderMapper orderMapper;
    private final CoinRecordService coinRecordService;
    private final EarningsService earningsService;
    private final MessageService messageService;
    private final LotteryCampaignMapper lotteryCampaignMapper;
    private final LotteryChanceService lotteryChanceService;

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
        createLotteryInviteChance(invitee, inviter, relation.getId());
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

        String inviterName = StringUtils.hasText(inviter.getNickname()) ? inviter.getNickname() : inviter.getEmail();
        String description = String.format("接受 %s 的邀请注册，获得 %s 创作币奖励",
                inviterName, NEW_USER_COIN_BONUS.toPlainString());
        earningsService.recordEarnings(invitee.getId(), EarningsType.INVITE_REWARD.getCode(),
                "invite_register", inviter.getId().toString(), "邀请注册奖励", description,
                NEW_USER_COIN_BONUS, YearMonth.now().toString());

        long count = userInviteRelationMapper.selectCount(
                new LambdaQueryWrapper<UserInviteRelation>()
                        .eq(UserInviteRelation::getInviterId, inviter.getId())
                        .eq(UserInviteRelation::getEffectiveStatus, EFFECTIVE_STATUS)
        );
        BigDecimal rewardCoins = calculateInviteRewardCoins(count);
        if (rewardCoins.compareTo(BigDecimal.ZERO) > 0) {
            String coinBizNo = coinRecordService.grant(inviter.getId(), COIN_BIZ_TYPE_LADDER_REWARD, rewardCoins,
                    null, "邀请阶梯奖励");
            String ladderDescription = String.format("累计邀请 %d 位好友，获得 %s 创作币阶梯奖励",
                    count, rewardCoins.toPlainString());
            earningsService.recordEarnings(inviter.getId(), EarningsType.INVITE_REWARD.getCode(),
                    "invite_ladder", coinBizNo, "邀请阶梯奖励", ladderDescription, rewardCoins,
                    YearMonth.now().toString());
            sendInviteRewardMessage(inviter, count, rewardCoins);
            log.info("邀请人 {} 累计有效邀请达到阶梯，获得 {} 创作币", inviter.getEmail(), rewardCoins.toPlainString());
        }
    }

    private void createLotteryInviteChance(User invitee, User inviter, Long relationId) {
        LotteryCampaign activeCampaign = lotteryCampaignMapper.selectOne(
                new LambdaQueryWrapper<LotteryCampaign>()
                        .eq(LotteryCampaign::getStatus, 1)
                        .le(LotteryCampaign::getStartTime, LocalDateTime.now())
                        .ge(LotteryCampaign::getEndTime, LocalDateTime.now())
                        .last("LIMIT 1"));
        if (activeCampaign == null) {
            return;
        }
        lotteryChanceService.createInviteChance(activeCampaign.getId(), inviter.getId(), relationId);
        log.info("邀请人 {} 因被邀请人 {} 注册获得抽奖次数", inviter.getId(), invitee.getId());
    }

    private void sendInviteRewardMessage(User inviter, long count, BigDecimal rewardCoins) {
        String summary = String.format("恭喜！您累计邀请 %d 位好友，获得 %s 创作币", count, rewardCoins.toPlainString());
        String content = String.format(
                "亲爱的用户：\n\n恭喜您累计邀请 %d 位好友加入爱创作，系统已为您发放 %s 创作币奖励。\n\n"
                        + "奖励已到账，您可前往「我的」页面查看创作币余额。\n\n感谢您的分享！",
                count, rewardCoins.toPlainString());
        messageService.pushPersonal(inviter.getId(), "coin", "邀请奖励到账",
                summary, null, content, MessageSubType.INVITE_REWARD.getCode());
    }

    /**
     * 根据邀请人当前累计有效邀请数，计算本次应额外奖励的创作币数量。
     *
     * <p>阶梯规则：第 3 人 +30 创作币，第 5 人 +50 创作币，第 6 人起每多 1 人 +20 创作币。
     *
     * @param count 累计有效邀请数
     * @return 本次应奖励的创作币数量；不触发阶梯时返回 0
     */
    private BigDecimal calculateInviteRewardCoins(long count) {
        if (count == 3) {
            return new BigDecimal("30");
        }
        if (count == 5) {
            return new BigDecimal("50");
        }
        if (count > 5) {
            return new BigDecimal("20");
        }
        return BigDecimal.ZERO;
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
        vo.setInviteCoinEarned(calculateTotalInviteCoins(relations.size()));

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
     * 根据累计有效邀请数计算累计获得的邀请阶梯奖励创作币。
     *
     * <p>规则：3 人 +30 创作币，5 人 +50 创作币，>5 人后每多 1 人 +20 创作币。
     * 示例：6 人 = 30 + 50 + 20 = 100 创作币。
     *
     * @param count 累计有效邀请数
     * @return 累计邀请阶梯奖励创作币
     */
    private int calculateTotalInviteCoins(int count) {
        int coins = 0;
        if (count >= 3) {
            coins += 30;
        }
        if (count >= 5) {
            coins += 50;
        }
        if (count > 5) {
            coins += (count - 5) * 20;
        }
        return coins;
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
