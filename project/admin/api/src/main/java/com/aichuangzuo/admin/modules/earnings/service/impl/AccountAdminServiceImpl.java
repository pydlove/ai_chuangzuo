package com.aichuangzuo.admin.modules.earnings.service.impl;

import com.aichuangzuo.admin.modules.earnings.dto.request.AccountQueryRequest;
import com.aichuangzuo.admin.modules.earnings.dto.request.UserCoinRecordQueryRequest;
import com.aichuangzuo.admin.modules.earnings.dto.request.UserEarningsRecordQueryRequest;
import com.aichuangzuo.admin.modules.earnings.dto.request.UserRewardRecordQueryRequest;
import com.aichuangzuo.admin.modules.earnings.entity.EarningsRecord;
import com.aichuangzuo.admin.modules.earnings.entity.UserCoinRecord;
import com.aichuangzuo.admin.modules.earnings.mapper.AccountAdminMapper;
import com.aichuangzuo.admin.modules.earnings.mapper.EarningsRecordMapper;
import com.aichuangzuo.admin.modules.earnings.mapper.UserCoinRecordMapper;
import com.aichuangzuo.admin.modules.leaderboard.entity.RewardRecord;
import com.aichuangzuo.admin.modules.leaderboard.mapper.RewardRecordMapper;
import com.aichuangzuo.admin.modules.earnings.service.AccountAdminService;
import com.aichuangzuo.admin.modules.earnings.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountAdminServiceImpl implements AccountAdminService {

    private final AccountAdminMapper accountAdminMapper;
    private final EarningsRecordMapper earningsRecordMapper;
    private final UserCoinRecordMapper userCoinRecordMapper;
    private final RewardRecordMapper rewardRecordMapper;

    @Override
    public UserAccountPageVO listAccounts(AccountQueryRequest request) {
        long offset = (request.getPage() - 1L) * request.getSize();
        List<UserAccountVO> list = accountAdminMapper.selectAccountList(
                request.getUserId(),
                request.getNickname(),
                request.getPhone(),
                request.getEmail(),
                request.getSortBy(),
                offset,
                request.getSize());

        YearMonth now = YearMonth.now();
        String month = now.toString();
        for (UserAccountVO vo : list) {
            vo.setCoinRankThisMonth(accountAdminMapper.selectCoinRank(vo.getUserId(), month));
            vo.setIncomeRankThisMonth(accountAdminMapper.selectIncomeRank(vo.getUserId(), month));
        }

        long total = accountAdminMapper.countAccountList(
                request.getUserId(), request.getNickname(), request.getPhone(), request.getEmail());

        UserAccountPageVO vo = new UserAccountPageVO();
        vo.setList(list);
        vo.setTotal(total);
        return vo;
    }

    @Override
    public UserAccountDetailVO getAccountDetail(Long userId) {
        UserAccountDetailVO detail = new UserAccountDetailVO();
        detail.setUserId(userId);

        List<UserAccountVO> accountList = accountAdminMapper.selectAccountList(userId, null, null, null, null, 0, 1);
        if (!accountList.isEmpty()) {
            UserAccountVO account = accountList.get(0);
            detail.setNickname(account.getNickname());
            detail.setEmail(account.getEmail());
            detail.setRegisteredAt(account.getRegisteredAt());
            detail.setTotalEarnings(account.getTotalEarnings());
            detail.setUnsettledEarnings(account.getTotalEarnings());
            detail.setSettledEarnings(BigDecimal.ZERO);
            detail.setCoinBalance(account.getCoinBalance());
        }

        LambdaQueryWrapper<EarningsRecord> earningsWrapper = Wrappers.lambdaQuery();
        earningsWrapper.eq(EarningsRecord::getUserId, userId)
                .eq(EarningsRecord::getIsDeleted, 0)
                .orderByDesc(EarningsRecord::getCreatedAt)
                .last("LIMIT 10");
        List<EarningsRecord> earnings = earningsRecordMapper.selectList(earningsWrapper);

        LambdaQueryWrapper<UserCoinRecord> coinWrapper = Wrappers.lambdaQuery();
        coinWrapper.eq(UserCoinRecord::getUserId, userId)
                .eq(UserCoinRecord::getIsDeleted, 0)
                .orderByDesc(UserCoinRecord::getBizTime)
                .last("LIMIT 10");
        List<UserCoinRecord> coins = userCoinRecordMapper.selectList(coinWrapper);

        BigDecimal totalCoinIncome = coins.stream()
                .filter(c -> c.getDirection() != null && c.getDirection() == 1)
                .map(UserCoinRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCoinExpense = coins.stream()
                .filter(c -> c.getDirection() != null && c.getDirection() == 2)
                .map(UserCoinRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LambdaQueryWrapper<RewardRecord> rewardWrapper = Wrappers.lambdaQuery();
        rewardWrapper.eq(RewardRecord::getUserId, userId)
                .eq(RewardRecord::getIsDeleted, 0)
                .orderByDesc(RewardRecord::getGrantedAt)
                .last("LIMIT 10");
        List<RewardRecord> rewards = rewardRecordMapper.selectList(rewardWrapper);

        detail.setTotalCoinIncome(totalCoinIncome);
        detail.setTotalCoinExpense(totalCoinExpense);
        detail.setRewardCount(rewardRecordMapper.selectCount(
                Wrappers.lambdaQuery(RewardRecord.class).eq(RewardRecord::getUserId, userId)).intValue());
        detail.setRecentEarnings(earnings.stream().map(this::toEarningsRecordVO).collect(Collectors.toList()));
        detail.setRecentCoinRecords(coins.stream().map(this::toUserCoinRecordVO).collect(Collectors.toList()));
        detail.setRecentRewards(rewards.stream().map(this::toRewardRecordVO).collect(Collectors.toList()));
        return detail;
    }

    @Override
    public PageResult<UserCoinRecordVO> listUserCoinRecords(Long userId, UserCoinRecordQueryRequest request) {
        Page<UserCoinRecord> page = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<UserCoinRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(UserCoinRecord::getUserId, userId)
                .eq(UserCoinRecord::getIsDeleted, 0);
        if (request.getDirection() != null) {
            wrapper.eq(UserCoinRecord::getDirection, request.getDirection());
        }
        if (request.getStartTime() != null) {
            wrapper.ge(UserCoinRecord::getBizTime, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le(UserCoinRecord::getBizTime, request.getEndTime());
        }
        wrapper.orderByDesc(UserCoinRecord::getBizTime);
        Page<UserCoinRecord> result = userCoinRecordMapper.selectPage(page, wrapper);
        List<UserCoinRecordVO> items = result.getRecords().stream()
                .map(this::toUserCoinRecordVO)
                .collect(Collectors.toList());
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<EarningsRecordVO> listUserEarningsRecords(Long userId, UserEarningsRecordQueryRequest request) {
        Page<EarningsRecord> page = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<EarningsRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(EarningsRecord::getUserId, userId)
                .eq(EarningsRecord::getIsDeleted, 0);
        if (request.getType() != null && !request.getType().isBlank()) {
            wrapper.eq(EarningsRecord::getType, request.getType());
        }
        if (request.getStartTime() != null) {
            wrapper.ge(EarningsRecord::getCreatedAt, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le(EarningsRecord::getCreatedAt, request.getEndTime());
        }
        wrapper.orderByDesc(EarningsRecord::getCreatedAt);
        Page<EarningsRecord> result = earningsRecordMapper.selectPage(page, wrapper);
        List<EarningsRecordVO> items = result.getRecords().stream()
                .map(this::toEarningsRecordVO)
                .collect(Collectors.toList());
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<RewardRecordVO> listUserRewardRecords(Long userId, UserRewardRecordQueryRequest request) {
        Page<RewardRecord> page = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<RewardRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(RewardRecord::getUserId, userId)
                .eq(RewardRecord::getIsDeleted, 0);
        if (request.getLeaderboardType() != null) {
            wrapper.eq(RewardRecord::getLeaderboardType, request.getLeaderboardType());
        }
        if (request.getStartTime() != null) {
            wrapper.ge(RewardRecord::getGrantedAt, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le(RewardRecord::getGrantedAt, request.getEndTime());
        }
        wrapper.orderByDesc(RewardRecord::getGrantedAt);
        Page<RewardRecord> result = rewardRecordMapper.selectPage(page, wrapper);
        List<RewardRecordVO> items = result.getRecords().stream()
                .map(this::toRewardRecordVO)
                .collect(Collectors.toList());
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private EarningsRecordVO toEarningsRecordVO(EarningsRecord e) {
        EarningsRecordVO vo = new EarningsRecordVO();
        vo.setId(e.getId());
        vo.setType(e.getType());
        vo.setTitle(e.getTitle());
        vo.setAmount(e.getAmount());
        vo.setSettlementMonth(e.getSettlementMonth());
        vo.setCreatedAt(e.getCreatedAt());
        return vo;
    }

    private UserCoinRecordVO toUserCoinRecordVO(UserCoinRecord c) {
        UserCoinRecordVO vo = new UserCoinRecordVO();
        vo.setId(c.getId());
        vo.setBizType(c.getBizType());
        vo.setDirection(c.getDirection());
        vo.setAmount(c.getAmount());
        vo.setBalanceAfter(c.getBalanceAfter());
        vo.setRemark(c.getRemark());
        vo.setBizTime(c.getBizTime());
        return vo;
    }

    private RewardRecordVO toRewardRecordVO(RewardRecord r) {
        RewardRecordVO vo = new RewardRecordVO();
        vo.setId(r.getId());
        vo.setLeaderboardType(r.getLeaderboardType());
        vo.setPeriodMonth(r.getPeriodMonth());
        vo.setRankNo(r.getRankNo());
        vo.setAmount(r.getAmount());
        vo.setGrantedAt(r.getGrantedAt());
        return vo;
    }
}
