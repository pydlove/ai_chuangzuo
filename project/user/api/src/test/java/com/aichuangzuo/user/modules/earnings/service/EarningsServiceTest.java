package com.aichuangzuo.user.modules.earnings.service;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.earnings.dto.request.ListEarningsRequest;
import com.aichuangzuo.user.modules.earnings.entity.EarningsRecord;
import com.aichuangzuo.user.modules.earnings.enums.EarningsStatus;
import com.aichuangzuo.user.modules.earnings.enums.EarningsType;
import com.aichuangzuo.user.modules.earnings.mapper.EarningsRecordMapper;
import com.aichuangzuo.user.modules.earnings.vo.AccountSummaryVO;
import com.aichuangzuo.user.modules.earnings.vo.EarningsRecordPageVO;
import com.aichuangzuo.user.modules.earnings.vo.MonthlySettlementVO;
import com.aichuangzuo.user.modules.earnings.vo.SettleLastMonthResultVO;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class EarningsServiceTest {

    @Autowired
    private EarningsService earningsService;

    @Autowired
    private EarningsRecordMapper earningsRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CoinRecordService coinRecordService;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Test
    void getSummary_shouldReturnZerosForNewUser() {
        User user = createUser("earnings-summary-zero@test.com");

        AccountSummaryVO summary = earningsService.getSummary(user.getId());

        assertNotNull(summary);
        assertEquals(0, summary.getCoinBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, summary.getTotalEarnings().compareTo(BigDecimal.ZERO));
        assertEquals(0, summary.getSettledEarnings().compareTo(BigDecimal.ZERO));
        assertEquals(0, summary.getUnsettledEarnings().compareTo(BigDecimal.ZERO));
    }

    @Test
    void getSummary_shouldComputeTotalsCorrectly() {
        User user = createUser("earnings-summary@test.com");
        String month = currentMonth();
        createEarningsRecord(user.getId(), EarningsType.USAGE.getCode(), month, new BigDecimal("10.00"), EarningsStatus.SETTLED.getCode());
        createEarningsRecord(user.getId(), EarningsType.USAGE.getCode(), month, new BigDecimal("20.00"), EarningsStatus.UNSETTLED.getCode());
        createEarningsRecord(user.getId(), EarningsType.MILESTONE.getCode(), month, new BigDecimal("30.00"), EarningsStatus.UNSETTLED.getCode());

        AccountSummaryVO summary = earningsService.getSummary(user.getId());

        assertEquals(0, summary.getTotalEarnings().compareTo(new BigDecimal("60.00")));
        assertEquals(0, summary.getSettledEarnings().compareTo(new BigDecimal("10.00")));
        assertEquals(0, summary.getUnsettledEarnings().compareTo(new BigDecimal("50.00")));
    }

    @Test
    void getMonthlySettlementList_shouldAggregateByMonth() {
        User user = createUser("earnings-monthly@test.com");
        String month = currentMonth();
        createEarningsRecord(user.getId(), EarningsType.USAGE.getCode(), month, new BigDecimal("10.00"), EarningsStatus.SETTLED.getCode());
        createEarningsRecord(user.getId(), EarningsType.USAGE.getCode(), month, new BigDecimal("20.00"), EarningsStatus.UNSETTLED.getCode());
        String lastMonth = previousMonth();
        createEarningsRecord(user.getId(), EarningsType.USAGE.getCode(), lastMonth, new BigDecimal("15.00"), EarningsStatus.UNSETTLED.getCode());

        List<MonthlySettlementVO> list = earningsService.getMonthlySettlementList(user.getId());

        assertEquals(2, list.size());
        MonthlySettlementVO current = list.get(0);
        assertEquals(month, current.getMonth());
        assertEquals(2, current.getCount());
        assertEquals(0, current.getTotal().compareTo(new BigDecimal("30.00")));
        assertEquals(0, current.getSettled().compareTo(new BigDecimal("10.00")));
        assertEquals(0, current.getUnsettled().compareTo(new BigDecimal("20.00")));
    }

    @Test
    void listEarnings_shouldFilterAndPage() {
        User user = createUser("earnings-list@test.com");
        String month = currentMonth();
        createEarningsRecord(user.getId(), EarningsType.USAGE.getCode(), month, new BigDecimal("10.00"), EarningsStatus.SETTLED.getCode());
        createEarningsRecord(user.getId(), EarningsType.USAGE.getCode(), month, new BigDecimal("20.00"), EarningsStatus.UNSETTLED.getCode());
        createEarningsRecord(user.getId(), EarningsType.MILESTONE.getCode(), month, new BigDecimal("30.00"), EarningsStatus.UNSETTLED.getCode());

        ListEarningsRequest request = new ListEarningsRequest();
        request.setStatus("unsettled");
        request.setPage(1);
        request.setPageSize(10);

        EarningsRecordPageVO page = earningsService.listEarnings(user.getId(), request);

        assertEquals(2, page.getTotal());
        assertEquals(2, page.getList().size());
        assertTrue(page.getList().stream().allMatch(r -> r.getStatus().equals(EarningsStatus.UNSETTLED.getCode())));
    }

    @Test
    void settleLastMonth_shouldSettlePendingRecordsAndGrantCoins() {
        User user = createUser("earnings-settle@test.com");
        String lastMonth = previousMonth();
        createEarningsRecord(user.getId(), EarningsType.USAGE.getCode(), lastMonth, new BigDecimal("10.00"), EarningsStatus.UNSETTLED.getCode());
        createEarningsRecord(user.getId(), EarningsType.MILESTONE.getCode(), lastMonth, new BigDecimal("20.00"), EarningsStatus.UNSETTLED.getCode());
        createEarningsRecord(user.getId(), EarningsType.USAGE.getCode(), currentMonth(), new BigDecimal("30.00"), EarningsStatus.UNSETTLED.getCode());

        SettleLastMonthResultVO result = earningsService.settleLastMonth(user.getId());

        assertEquals(lastMonth, result.getMonth());
        assertEquals(2, result.getSettledCount());
        assertEquals(0, result.getSettledAmount().compareTo(new BigDecimal("30.00")));
        assertEquals(0, coinRecordService.getBalance(user.getId()).compareTo(new BigDecimal("30.00")));

        long unsettledLastMonth = earningsRecordMapper.selectCount(
                new LambdaQueryWrapper<EarningsRecord>()
                        .eq(EarningsRecord::getUserId, user.getId())
                        .eq(EarningsRecord::getSettlementMonth, lastMonth)
                        .eq(EarningsRecord::getStatus, EarningsStatus.UNSETTLED.getCode())
                        .eq(EarningsRecord::getIsDeleted, 0));
        assertEquals(0, unsettledLastMonth);
    }

    @Test
    void settleLastMonth_shouldBeIdempotent() {
        User user = createUser("earnings-settle-idempotent@test.com");
        String lastMonth = previousMonth();
        createEarningsRecord(user.getId(), EarningsType.USAGE.getCode(), lastMonth, new BigDecimal("10.00"), EarningsStatus.UNSETTLED.getCode());

        SettleLastMonthResultVO first = earningsService.settleLastMonth(user.getId());
        assertEquals(1, first.getSettledCount());

        SettleLastMonthResultVO second = earningsService.settleLastMonth(user.getId());
        assertEquals(0, second.getSettledCount());
        assertEquals(0, second.getSettledAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, coinRecordService.getBalance(user.getId()).compareTo(new BigDecimal("10.00")));
    }

    private User createUser(String email) {
        User user = new User();
        user.setBizNo("B" + System.nanoTime());
        user.setEmail(email);
        user.setPasswordHash("x");
        user.setInviteCode("X" + System.nanoTime());
        user.setUserStatus(1);
        user.setEmailVerified(1);
        userMapper.insert(user);
        return user;
    }

    private void createEarningsRecord(Long userId, String type, String month, BigDecimal amount, Integer status) {
        EarningsRecord record = new EarningsRecord();
        record.setUserId(userId);
        record.setType(type);
        record.setSourceType("test");
        record.setTitle("测试收益");
        record.setDescription("测试描述");
        record.setAmount(amount);
        record.setStatus(status);
        record.setSettlementMonth(month);
        earningsRecordMapper.insert(record);
    }

    private String currentMonth() {
        return LocalDate.now().format(MONTH_FORMATTER);
    }

    private String previousMonth() {
        return LocalDate.now().minusMonths(1).format(MONTH_FORMATTER);
    }
}
