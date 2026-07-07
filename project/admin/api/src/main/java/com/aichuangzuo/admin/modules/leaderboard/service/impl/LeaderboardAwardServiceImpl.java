package com.aichuangzuo.admin.modules.leaderboard.service.impl;

import com.aichuangzuo.admin.modules.leaderboard.client.UserApiClient;
import com.aichuangzuo.admin.modules.leaderboard.entity.LeaderboardType;
import com.aichuangzuo.admin.modules.leaderboard.entity.RewardRecord;
import com.aichuangzuo.admin.modules.leaderboard.enums.AdminLeaderboardErrorCode;
import com.aichuangzuo.admin.modules.leaderboard.mapper.LeaderboardAggregateMapper;
import com.aichuangzuo.admin.modules.leaderboard.mapper.RewardRecordMapper;
import com.aichuangzuo.admin.modules.leaderboard.service.LeaderboardAwardService;
import com.aichuangzuo.admin.modules.leaderboard.vo.LeaderboardGrantResultVO;
import com.aichuangzuo.admin.modules.leaderboard.vo.LeaderboardTop10VO;
import com.aichuangzuo.admin.modules.leaderboard.vo.RewardRecordAdminVO;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 收益排行榜发奖服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardAwardServiceImpl implements LeaderboardAwardService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int TOP_LIMIT = 10;
    private static final BigDecimal DEFAULT_REWARD = new BigDecimal("100.0000");

    private final LeaderboardAggregateMapper aggregateMapper;
    private final RewardRecordMapper rewardRecordMapper;
    private final UserApiClient userApiClient;

    @Override
    public List<LeaderboardTop10VO> previewTop10(Integer leaderboardType, String periodMonth) {
        validateLeaderboardType(leaderboardType);
        parseMonth(periodMonth);

        List<LeaderboardTop10VO> list = switch (leaderboardType) {
            case 1 -> {
                YearMonth ym = YearMonth.parse(periodMonth, MONTH_FORMATTER);
                LocalDateTime start = ym.atDay(1).atStartOfDay();
                LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
                yield aggregateMapper.selectCoinRankingMonth(start, end, TOP_LIMIT);
            }
            case 2 -> aggregateMapper.selectIncomeRankingMonth(periodMonth, TOP_LIMIT);
            default -> throw new BusinessException(AdminLeaderboardErrorCode.SUBMISSION_NOT_FOUND);
        };

        for (int i = 0; i < list.size(); i++) {
            LeaderboardTop10VO item = list.get(i);
            item.setRank(i + 1);
            item.setRewardAmount(DEFAULT_REWARD);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LeaderboardGrantResultVO grant(Integer leaderboardType, String periodMonth, Long adminUserId) {
        List<LeaderboardTop10VO> top10 = previewTop10(leaderboardType, periodMonth);
        int granted = 0;
        int skipped = 0;
        String typeName = leaderboardType == LeaderboardType.COIN.getCode() ? "创作币榜" : "自媒体收入榜";

        for (LeaderboardTop10VO item : top10) {
            if (rewardRecordMapper.exists(leaderboardType, periodMonth, item.getUserId())) {
                skipped++;
                continue;
            }
            String remark = String.format("%s %s 月度第 %d 名奖励", typeName, periodMonth, item.getRank());
            String coinRecordBizNo = userApiClient.grantCoin(item.getUserId(), "leaderboard_reward",
                    item.getRewardAmount(), null, remark);

            RewardRecord record = new RewardRecord();
            record.setBizNo("LR" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
            record.setLeaderboardType(leaderboardType);
            record.setPeriodMonth(periodMonth);
            record.setRankNo(item.getRank());
            record.setUserId(item.getUserId());
            record.setAmount(item.getRewardAmount());
            record.setCoinRecordBizNo(coinRecordBizNo);
            record.setGrantedBy(adminUserId);
            record.setGrantedAt(LocalDateTime.now());
            record.setCreatedBy(adminUserId);
            record.setUpdatedBy(adminUserId);
            rewardRecordMapper.insert(record);
            granted++;
        }
        return new LeaderboardGrantResultVO(granted, skipped);
    }

    @Override
    public IPage<RewardRecordAdminVO> rewardHistory(Integer leaderboardType, String periodMonth, IPage<RewardRecordAdminVO> pageParam) {
        LambdaQueryWrapper<RewardRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(RewardRecord::getIsDeleted, 0);
        if (leaderboardType != null) {
            wrapper.eq(RewardRecord::getLeaderboardType, leaderboardType);
        }
        if (periodMonth != null && !periodMonth.isBlank()) {
            wrapper.eq(RewardRecord::getPeriodMonth, periodMonth);
        }
        wrapper.orderByDesc(RewardRecord::getGrantedAt);
        Page<RewardRecord> entityPage = new Page<>(pageParam.getCurrent(), pageParam.getSize());
        rewardRecordMapper.selectPage(entityPage, wrapper);
        return entityPage.convert(this::toRewardRecordVo);
    }

    private RewardRecordAdminVO toRewardRecordVo(RewardRecord entity) {
        RewardRecordAdminVO vo = new RewardRecordAdminVO();
        vo.setId(entity.getId());
        vo.setBizNo(entity.getBizNo());
        vo.setLeaderboardType(entity.getLeaderboardType());
        vo.setPeriodMonth(entity.getPeriodMonth());
        vo.setRankNo(entity.getRankNo());
        vo.setUserId(entity.getUserId());
        vo.setAmount(entity.getAmount());
        vo.setCoinRecordBizNo(entity.getCoinRecordBizNo());
        vo.setGrantedBy(entity.getGrantedBy());
        vo.setGrantedAt(entity.getGrantedAt());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    private void validateLeaderboardType(Integer leaderboardType) {
        if (leaderboardType == null ||
                (leaderboardType != LeaderboardType.COIN.getCode() && leaderboardType != LeaderboardType.INCOME.getCode())) {
            throw new BusinessException(AdminLeaderboardErrorCode.SUBMISSION_NOT_FOUND);
        }
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month, MONTH_FORMATTER);
        } catch (Exception e) {
            throw new BusinessException(AdminLeaderboardErrorCode.SUBMISSION_NOT_FOUND);
        }
    }
}
