package com.aichuangzuo.admin.modules.earnings.service.impl;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.earnings.client.UserCoinRecordClient;
import com.aichuangzuo.admin.modules.earnings.dto.request.LeaderboardGrantRequest;
import com.aichuangzuo.admin.modules.earnings.entity.RewardRecord;
import com.aichuangzuo.admin.modules.earnings.mapper.LeaderboardAggregateMapper;
import com.aichuangzuo.admin.modules.earnings.mapper.RewardRecordMapper;
import com.aichuangzuo.admin.modules.earnings.service.LeaderboardAwardService;
import com.aichuangzuo.admin.modules.earnings.vo.LeaderboardTop10VO;
import com.aichuangzuo.admin.modules.earnings.vo.RewardRecordAdminVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaderboardAwardServiceImpl implements LeaderboardAwardService {

    private final RewardRecordMapper rewardRecordMapper;
    private final LeaderboardAggregateMapper leaderboardAggregateMapper;
    private final UserCoinRecordClient userCoinRecordClient;

    private static final BigDecimal REWARD_AMOUNT = new BigDecimal("100.00");

    @Override
    public List<LeaderboardTop10VO> preview(Integer leaderboardType, String periodMonth) {
        List<LeaderboardTop10VO> top10 = switch (leaderboardType) {
            case 1 -> leaderboardAggregateMapper.selectCoinTop10(periodMonth);
            case 2 -> leaderboardAggregateMapper.selectIncomeTop10(periodMonth);
            default -> List.of();
        };

        LambdaQueryWrapper<RewardRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(RewardRecord::getLeaderboardType, leaderboardType)
                .eq(RewardRecord::getPeriodMonth, periodMonth)
                .eq(RewardRecord::getIsDeleted, 0);
        List<RewardRecord> granted = rewardRecordMapper.selectList(wrapper);

        for (LeaderboardTop10VO vo : top10) {
            boolean alreadyGranted = granted.stream()
                    .anyMatch(r -> r.getUserId().equals(vo.getUserId()));
            vo.setGranted(alreadyGranted);
        }
        return top10;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int grant(LeaderboardGrantRequest request) {
        List<LeaderboardTop10VO> top10 = preview(request.getLeaderboardType(), request.getPeriodMonth());
        int granted = 0;
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        for (LeaderboardTop10VO entry : top10) {
            if (Boolean.TRUE.equals(entry.getGranted())) {
                continue;
            }
            String bizNo = "LR" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

            RewardRecord record = new RewardRecord();
            record.setBizNo(bizNo);
            record.setLeaderboardType(request.getLeaderboardType());
            record.setPeriodMonth(request.getPeriodMonth());
            record.setRankNo(entry.getRank());
            record.setUserId(entry.getUserId());
            record.setAmount(REWARD_AMOUNT);
            record.setGrantedBy(adminId);
            record.setGrantedAt(LocalDateTime.now());
            record.setIsDeleted(0);
            record.setCreatedBy(adminId);
            record.setUpdatedBy(adminId);
            rewardRecordMapper.insert(record);

            String coinRecordBizNo = userCoinRecordClient.internalGrant(
                    entry.getUserId(), REWARD_AMOUNT, "leaderboard_reward", bizNo,
                    request.getPeriodMonth() + " 榜单第 " + entry.getRank() + " 名奖励");

            record.setCoinRecordBizNo(coinRecordBizNo);
            rewardRecordMapper.updateById(record);
            granted++;
        }
        return granted;
    }

    @Override
    public Page<RewardRecordAdminVO> listRewards(Integer leaderboardType, String periodMonth, int page, int size) {
        Page<RewardRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RewardRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(RewardRecord::getIsDeleted, 0);
        if (leaderboardType != null) {
            wrapper.eq(RewardRecord::getLeaderboardType, leaderboardType);
        }
        if (periodMonth != null && !periodMonth.isBlank()) {
            wrapper.eq(RewardRecord::getPeriodMonth, periodMonth);
        }
        wrapper.orderByDesc(RewardRecord::getGrantedAt);
        Page<RewardRecord> result = rewardRecordMapper.selectPage(pageParam, wrapper);

        Page<RewardRecordAdminVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVo).toList());
        return voPage;
    }

    private RewardRecordAdminVO toVo(RewardRecord entity) {
        RewardRecordAdminVO vo = new RewardRecordAdminVO();
        vo.setId(entity.getId());
        vo.setBizNo(entity.getBizNo());
        vo.setLeaderboardType(entity.getLeaderboardType());
        vo.setPeriodMonth(entity.getPeriodMonth());
        vo.setRankNo(entity.getRankNo());
        vo.setUserId(entity.getUserId());
        vo.setAmount(entity.getAmount());
        vo.setGrantedAt(entity.getGrantedAt());
        return vo;
    }
}
