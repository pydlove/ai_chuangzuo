package com.aichuangzuo.user.modules.leaderboard.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.shared.enums.error.LeaderboardErrorCode;
import com.aichuangzuo.user.modules.leaderboard.mapper.LeaderboardAggregateMapper;
import com.aichuangzuo.user.modules.leaderboard.mapper.LeaderboardRewardConfigMapper;
import com.aichuangzuo.user.modules.leaderboard.vo.CoinLeaderboardVO;
import com.aichuangzuo.user.modules.leaderboard.vo.IncomeLeaderboardVO;
import com.aichuangzuo.user.modules.leaderboard.vo.LeaderboardEntryVO;
import com.aichuangzuo.user.modules.leaderboard.vo.LeaderboardRewardConfigVO;
import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.aichuangzuo.user.modules.membership.mapper.UserMembershipMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 榜单聚合服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int TOP_LIMIT = 20;
    private static final int DEFAULT_REWARD_TOP_LIMIT = 3;
    private static final BigDecimal DEFAULT_REWARD_AMOUNT = new BigDecimal("500.0000");

    private final LeaderboardAggregateMapper aggregateMapper;
    private final UserMapper userMapper;
    private final LeaderboardRewardConfigMapper rewardConfigMapper;
    private final UserMembershipMapper userMembershipMapper;

    @Override
    public CoinLeaderboardVO getCoinLeaderboard(Long currentUserId, String month) {
        List<LeaderboardEntryVO> topList;
        Function<Long, LeaderboardEntryVO> meSupplier;

        if ("all".equalsIgnoreCase(month)) {
            topList = aggregateMapper.selectCoinRankingAll(TOP_LIMIT);
            meSupplier = userId -> aggregateMapper.selectCoinAmountByUserAll(userId);
        } else {
            YearMonth ym = parseMonth(month);
            LocalDateTime start = ym.atDay(1).atStartOfDay();
            LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
            topList = aggregateMapper.selectCoinRanking(start, end, TOP_LIMIT);
            meSupplier = userId -> aggregateMapper.selectCoinAmountByUser(userId, start, end);
        }

        fillUserInfo(topList);
        rank(topList);
        markMe(topList, currentUserId);

        CoinLeaderboardVO vo = new CoinLeaderboardVO();
        vo.setMonth(month);
        vo.setTopList(topList);
        vo.setMe(findMe(topList, currentUserId, meSupplier));
        return vo;
    }

    @Override
    public IncomeLeaderboardVO getIncomeLeaderboard(Long currentUserId, String periodType, String periodValue) {
        List<LeaderboardEntryVO> topList;
        Function<Long, LeaderboardEntryVO> meSupplier;

        if ("month".equalsIgnoreCase(periodType)) {
            parseMonth(periodValue);
            topList = aggregateMapper.selectIncomeRankingMonth(periodValue, TOP_LIMIT);
            meSupplier = userId -> aggregateMapper.selectIncomeAmountByUserMonth(userId, periodValue);
        } else if ("year".equalsIgnoreCase(periodType)) {
            if (!periodValue.matches("^\\d{4}$")) {
                throw new BusinessException(LeaderboardErrorCode.INCOME_PERIOD_INVALID);
            }
            topList = aggregateMapper.selectIncomeRankingYear(periodValue, TOP_LIMIT);
            meSupplier = userId -> aggregateMapper.selectIncomeAmountByUserYear(userId, periodValue);
        } else {
            throw new BusinessException(LeaderboardErrorCode.INCOME_PERIOD_INVALID);
        }

        fillUserInfo(topList);
        rank(topList);
        markMe(topList, currentUserId);

        IncomeLeaderboardVO vo = new IncomeLeaderboardVO();
        vo.setPeriodType(periodType);
        vo.setPeriodValue(periodValue);
        vo.setTopList(topList);
        vo.setMe(findMe(topList, currentUserId, meSupplier));
        return vo;
    }

    @Override
    public CoinLeaderboardVO getInviteLeaderboard(Long currentUserId) {
        // SQL 侧已 JOIN 有效邀请关系并过滤邀请 0 人的用户
        List<LeaderboardEntryVO> topList = aggregateMapper.selectInviteRanking(TOP_LIMIT);
        fillUserInfo(topList);
        rank(topList);
        markMe(topList, currentUserId);

        CoinLeaderboardVO vo = new CoinLeaderboardVO();
        vo.setMonth("all");
        vo.setTopList(topList);
        vo.setMe(findMe(topList, currentUserId, aggregateMapper::selectInviteAmountByUser));
        return vo;
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month, MONTH_FORMATTER);
        } catch (Exception e) {
            throw new BusinessException(LeaderboardErrorCode.INCOME_PERIOD_INVALID);
        }
    }

    private void fillUserInfo(List<LeaderboardEntryVO> list) {
        if (list.isEmpty()) {
            return;
        }
        Set<Long> userIds = list.stream()
                .map(LeaderboardEntryVO::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        Map<Long, String> memberLevelMap = loadValidMemberLevelMap(userIds);
        for (LeaderboardEntryVO entry : list) {
            User user = userMap.get(entry.getUserId());
            if (user != null) {
                entry.setNickname(user.getNickname());
                entry.setAvatarUrl(user.getAvatarUrl());
            }
            entry.setMemberLevel(memberLevelMap.get(entry.getUserId()));
            if (entry.getAmount() == null) {
                entry.setAmount(BigDecimal.ZERO);
            }
        }
    }

    /**
     * 批量查询用户当前有效会员等级。
     *
     * @return userId -> 会员等级（basic/pro/flagship）；非会员或已过期不放入 Map
     */
    private Map<Long, String> loadValidMemberLevelMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMembershipMapper.selectList(
                        new LambdaQueryWrapper<UserMembership>()
                                .in(UserMembership::getUserId, userIds)
                                .ge(UserMembership::getExpiresAt, LocalDate.now()))
                .stream()
                .collect(Collectors.toMap(UserMembership::getUserId, UserMembership::getLevel, (a, b) -> a));
    }

    private void rank(List<LeaderboardEntryVO> list) {
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setRank(i + 1);
        }
    }

    private void markMe(List<LeaderboardEntryVO> list, Long currentUserId) {
        for (LeaderboardEntryVO entry : list) {
            entry.setIsMe(entry.getUserId() != null && entry.getUserId().equals(currentUserId));
        }
    }

    private LeaderboardEntryVO findMe(List<LeaderboardEntryVO> topList, Long currentUserId,
                                      Function<Long, LeaderboardEntryVO> amountSupplier) {
        for (LeaderboardEntryVO entry : topList) {
            if (Boolean.TRUE.equals(entry.getIsMe())) {
                return entry;
            }
        }
        LeaderboardEntryVO me = amountSupplier.apply(currentUserId);
        if (me == null || me.getAmount() == null) {
            me = new LeaderboardEntryVO();
            me.setUserId(currentUserId);
            me.setAmount(BigDecimal.ZERO);
        }
        User user = userMapper.selectById(currentUserId);
        if (user != null) {
            me.setNickname(user.getNickname());
            me.setAvatarUrl(user.getAvatarUrl());
        }
        if (currentUserId != null) {
            me.setMemberLevel(loadValidMemberLevelMap(Set.of(currentUserId)).get(currentUserId));
        }
        me.setIsMe(true);
        me.setRank(null);
        return me;
    }

    @Override
    public LeaderboardRewardConfigVO getRewardConfig() {
        com.aichuangzuo.user.modules.leaderboard.entity.LeaderboardRewardConfig config = rewardConfigMapper.selectById(1L);
        LeaderboardRewardConfigVO vo = new LeaderboardRewardConfigVO();
        if (config == null) {
            vo.setTopLimit(DEFAULT_REWARD_TOP_LIMIT);
            vo.setRewardAmount(DEFAULT_REWARD_AMOUNT);
        } else {
            vo.setTopLimit(config.getRewardTopLimit());
            vo.setRewardAmount(config.getRewardAmount());
        }
        return vo;
    }
}
