package com.aichuangzuo.user.modules.leaderboard.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.leaderboard.enums.LeaderboardErrorCode;
import com.aichuangzuo.user.modules.leaderboard.mapper.LeaderboardAggregateMapper;
import com.aichuangzuo.user.modules.leaderboard.vo.CoinLeaderboardVO;
import com.aichuangzuo.user.modules.leaderboard.vo.IncomeLeaderboardVO;
import com.aichuangzuo.user.modules.leaderboard.vo.LeaderboardEntryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    private final LeaderboardAggregateMapper aggregateMapper;
    private final UserMapper userMapper;

    @Override
    @Cacheable(value = "leaderboard", key = "'coin:' + #month + ':' + #currentUserId")
    public CoinLeaderboardVO getCoinLeaderboard(Long currentUserId, String month) {
        YearMonth ym = parseMonth(month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

        List<LeaderboardEntryVO> topList = aggregateMapper.selectCoinRanking(start, end, TOP_LIMIT);
        fillUserInfo(topList);
        rank(topList);
        markMe(topList, currentUserId);

        CoinLeaderboardVO vo = new CoinLeaderboardVO();
        vo.setMonth(month);
        vo.setTopList(topList);
        vo.setMe(findMe(topList, currentUserId, userId -> aggregateMapper.selectCoinAmountByUser(userId, start, end)));
        return vo;
    }

    @Override
    @Cacheable(value = "leaderboard", key = "'income:' + #periodType + ':' + #periodValue + ':' + #currentUserId")
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
        for (LeaderboardEntryVO entry : list) {
            User user = userMap.get(entry.getUserId());
            if (user != null) {
                entry.setNickname(user.getNickname());
                entry.setAvatarUrl(user.getAvatarUrl());
            }
            if (entry.getAmount() == null) {
                entry.setAmount(BigDecimal.ZERO);
            }
        }
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
        me.setIsMe(true);
        me.setRank(null);
        return me;
    }
}
