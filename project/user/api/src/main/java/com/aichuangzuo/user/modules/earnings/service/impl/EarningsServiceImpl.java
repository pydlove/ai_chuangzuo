package com.aichuangzuo.user.modules.earnings.service.impl;

import com.aichuangzuo.user.modules.earnings.dto.request.ListEarningsRequest;
import com.aichuangzuo.user.modules.earnings.entity.EarningsRecord;
import com.aichuangzuo.user.modules.earnings.enums.EarningsStatus;
import com.aichuangzuo.user.modules.earnings.enums.EarningsType;
import com.aichuangzuo.user.modules.earnings.mapper.EarningsRecordMapper;
import com.aichuangzuo.user.modules.earnings.service.EarningsService;
import com.aichuangzuo.user.modules.earnings.vo.AccountSummaryVO;
import com.aichuangzuo.user.modules.earnings.vo.EarningsRecordPageVO;
import com.aichuangzuo.user.modules.earnings.vo.EarningsRecordVO;
import com.aichuangzuo.user.modules.earnings.vo.MonthlySettlementVO;
import com.aichuangzuo.user.modules.earnings.vo.SettleLastMonthResultVO;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户收益服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EarningsServiceImpl implements EarningsService {

    private static final String SETTLE_BIZ_TYPE = "EARNINGS_SETTLE";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final EarningsRecordMapper earningsRecordMapper;
    private final CoinRecordService coinRecordService;

    @Override
    public AccountSummaryVO getSummary(Long userId) {
        LambdaQueryWrapper<EarningsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EarningsRecord::getUserId, userId)
                .eq(EarningsRecord::getIsDeleted, 0);

        List<EarningsRecord> records = earningsRecordMapper.selectList(wrapper);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal settled = BigDecimal.ZERO;
        BigDecimal unsettled = BigDecimal.ZERO;
        for (EarningsRecord record : records) {
            BigDecimal amount = record.getAmount() == null ? BigDecimal.ZERO : record.getAmount();
            total = total.add(amount);
            if (EarningsStatus.SETTLED.getCode() == record.getStatus()) {
                settled = settled.add(amount);
            } else {
                unsettled = unsettled.add(amount);
            }
        }

        AccountSummaryVO vo = new AccountSummaryVO();
        vo.setCoinBalance(coinRecordService.getBalance(userId));
        vo.setTotalEarnings(total);
        vo.setSettledEarnings(settled);
        vo.setUnsettledEarnings(unsettled);
        return vo;
    }

    @Override
    public List<MonthlySettlementVO> getMonthlySettlementList(Long userId) {
        return earningsRecordMapper.selectMonthlySettlementList(userId);
    }

    @Override
    public EarningsRecordPageVO listEarnings(Long userId, ListEarningsRequest request) {
        LambdaQueryWrapper<EarningsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EarningsRecord::getUserId, userId)
                .eq(EarningsRecord::getIsDeleted, 0);

        if (StringUtils.hasText(request.getMonth())) {
            wrapper.eq(EarningsRecord::getSettlementMonth, request.getMonth());
        }

        String status = request.getStatus();
        if ("settled".equals(status)) {
            wrapper.eq(EarningsRecord::getStatus, EarningsStatus.SETTLED.getCode());
        } else if ("unsettled".equals(status)) {
            wrapper.eq(EarningsRecord::getStatus, EarningsStatus.UNSETTLED.getCode());
        }

        wrapper.orderByDesc(EarningsRecord::getCreatedAt);

        IPage<EarningsRecord> page = new Page<>(request.getPage(), request.getPageSize());
        page = earningsRecordMapper.selectPage(page, wrapper);

        EarningsRecordPageVO vo = new EarningsRecordPageVO();
        vo.setList(page.getRecords().stream().map(this::toVo).collect(Collectors.toList()));
        vo.setTotal(page.getTotal());
        vo.setPage(page.getCurrent());
        vo.setPageSize(page.getSize());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettleLastMonthResultVO settleLastMonth(Long userId) {
        String lastMonth = getPreviousMonth();

        LambdaQueryWrapper<EarningsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EarningsRecord::getUserId, userId)
                .eq(EarningsRecord::getSettlementMonth, lastMonth)
                .eq(EarningsRecord::getStatus, EarningsStatus.UNSETTLED.getCode())
                .eq(EarningsRecord::getIsDeleted, 0);

        List<EarningsRecord> pendingRecords = earningsRecordMapper.selectList(wrapper);
        if (pendingRecords.isEmpty()) {
            SettleLastMonthResultVO emptyResult = new SettleLastMonthResultVO();
            emptyResult.setMonth(lastMonth);
            emptyResult.setSettledCount(0);
            emptyResult.setSettledAmount(BigDecimal.ZERO);
            return emptyResult;
        }

        BigDecimal totalAmount = pendingRecords.stream()
                .map(r -> r.getAmount() == null ? BigDecimal.ZERO : r.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        earningsRecordMapper.update(null, new LambdaUpdateWrapper<EarningsRecord>()
                .set(EarningsRecord::getStatus, EarningsStatus.SETTLED.getCode())
                .set(EarningsRecord::getSettledAt, LocalDateTime.now())
                .eq(EarningsRecord::getUserId, userId)
                .eq(EarningsRecord::getSettlementMonth, lastMonth)
                .eq(EarningsRecord::getStatus, EarningsStatus.UNSETTLED.getCode())
                .eq(EarningsRecord::getIsDeleted, 0));

        coinRecordService.grant(userId, SETTLE_BIZ_TYPE, totalAmount, null,
                "收益结算：" + lastMonth);

        SettleLastMonthResultVO vo = new SettleLastMonthResultVO();
        vo.setMonth(lastMonth);
        vo.setSettledCount(pendingRecords.size());
        vo.setSettledAmount(totalAmount);
        log.info("用户收益结算完成 userId={}, month={}, count={}, amount={}",
                userId, lastMonth, pendingRecords.size(), totalAmount);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordEarnings(Long userId, String type, String sourceType, String sourceId,
                               String title, String description, BigDecimal amount, String settlementMonth) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("收益金额必须大于 0");
        }
        if (!StringUtils.hasText(settlementMonth)) {
            throw new IllegalArgumentException("归属月份不能为空");
        }
        EarningsType earningsType = EarningsType.of(type);
        if (earningsType == null) {
            throw new IllegalArgumentException("收益类型非法：" + type);
        }

        EarningsRecord record = new EarningsRecord();
        record.setUserId(userId);
        record.setType(earningsType.getCode());
        record.setSourceType(sourceType);
        record.setSourceId(sourceId);
        record.setTitle(title);
        record.setDescription(description);
        record.setAmount(amount);
        record.setStatus(EarningsStatus.UNSETTLED.getCode());
        record.setSettlementMonth(settlementMonth);
        earningsRecordMapper.insert(record);
    }

    private EarningsRecordVO toVo(EarningsRecord record) {
        EarningsRecordVO vo = new EarningsRecordVO();
        vo.setId(record.getId());
        vo.setType(record.getType());
        EarningsType typeEnum = EarningsType.of(record.getType());
        vo.setTypeLabel(typeEnum == null ? record.getType() : typeEnum.getLabel());
        vo.setTitle(record.getTitle());
        vo.setDescription(record.getDescription());
        vo.setAmount(record.getAmount());
        vo.setStatus(record.getStatus());
        EarningsStatus statusEnum = EarningsStatus.of(record.getStatus());
        vo.setStatusLabel(statusEnum == null ? "" : statusEnum.getLabel());
        vo.setSettlementMonth(record.getSettlementMonth());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }

    private String getPreviousMonth() {
        return LocalDate.now().minusMonths(1).format(MONTH_FORMATTER);
    }
}
