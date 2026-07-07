package com.aichuangzuo.admin.modules.earnings.service.impl;

import com.aichuangzuo.admin.modules.earnings.dto.request.SettlementRequest;
import com.aichuangzuo.admin.modules.earnings.entity.EarningsRecord;
import com.aichuangzuo.admin.modules.earnings.mapper.EarningsRecordMapper;
import com.aichuangzuo.admin.modules.earnings.mapper.SettlementAdminMapper;
import com.aichuangzuo.admin.modules.earnings.service.SettlementAdminService;
import com.aichuangzuo.admin.modules.earnings.vo.PendingSettlementSummaryVO;
import com.aichuangzuo.admin.modules.earnings.vo.PendingSettlementUserVO;
import com.aichuangzuo.admin.modules.earnings.vo.SettlementResultVO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementAdminServiceImpl implements SettlementAdminService {

    private final SettlementAdminMapper settlementAdminMapper;
    private final EarningsRecordMapper earningsRecordMapper;

    @Override
    public PendingSettlementSummaryVO pendingSummary(String month) {
        List<PendingSettlementUserVO> users = settlementAdminMapper.selectPendingUsers(month);
        long userCount = settlementAdminMapper.countPendingUsers(month);
        long recordCount = users.stream().mapToLong(PendingSettlementUserVO::getRecordCount).sum();
        BigDecimal total = users.stream()
                .map(PendingSettlementUserVO::getUnsettledAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PendingSettlementSummaryVO vo = new PendingSettlementSummaryVO();
        vo.setMonth(month);
        vo.setUserCount(userCount);
        vo.setRecordCount(recordCount);
        vo.setTotalAmount(total);
        vo.setUsers(users);
        return vo;
    }

    @Override
    public List<PendingSettlementUserVO> pendingUsers(String month) {
        return settlementAdminMapper.selectPendingUsers(month);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementResultVO settle(SettlementRequest request) {
        String month = request.getMonth();
        List<Long> userIds = request.getUserIds() != null ? request.getUserIds() : List.of();

        List<PendingSettlementUserVO> before = settlementAdminMapper.selectPendingAmountBeforeSettle(month, userIds);
        BigDecimal settledAmount = before.stream()
                .map(PendingSettlementUserVO::getUnsettledAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int settledUserCount = before.size();
        int settledRecordCount = before.stream().mapToInt(PendingSettlementUserVO::getRecordCount).sum();

        LambdaUpdateWrapper<EarningsRecord> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(EarningsRecord::getStatus, 0)
                .eq(EarningsRecord::getSettlementMonth, month)
                .set(EarningsRecord::getStatus, 1)
                .set(EarningsRecord::getSettledAt, LocalDateTime.now());

        if (!userIds.isEmpty()) {
            wrapper.in(EarningsRecord::getUserId, userIds);
        }

        earningsRecordMapper.update(null, wrapper);

        SettlementResultVO vo = new SettlementResultVO();
        vo.setMonth(month);
        vo.setSettledUserCount(settledUserCount);
        vo.setSettledRecordCount(settledRecordCount);
        vo.setSettledAmount(settledAmount);
        return vo;
    }
}
