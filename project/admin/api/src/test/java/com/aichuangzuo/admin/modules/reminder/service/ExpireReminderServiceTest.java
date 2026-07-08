package com.aichuangzuo.admin.modules.reminder.service;

import com.aichuangzuo.admin.modules.reminder.service.impl.ExpireReminderServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpireReminderServiceTest {

    private final ExpireReminderServiceImpl service = new ExpireReminderServiceImpl(null, null, null, null, null, null);

    @Test
    void shouldReturn2DaysWhenToday708AndExpireIsNextDay00After710() {
        // 7/10 24:00 结束 → 存 7/11 00:00。今天 7/8，应剩 2 天。
        int days = service.calcRemainingDays(
                LocalDateTime.of(2026, 7, 11, 0, 0), LocalDate.of(2026, 7, 8));
        assertEquals(2, days);
    }

    @Test
    void shouldReturn0WhenExpireIsNextDay00AfterToday() {
        // 今天 7/10 24:00 结束（存 7/11 00:00），今天 7/10，应剩 0 天。
        int days = service.calcRemainingDays(
                LocalDateTime.of(2026, 7, 11, 0, 0), LocalDate.of(2026, 7, 10));
        assertEquals(0, days);
    }

    @Test
    void shouldReturnNegative1WhenAlreadyExpired() {
        // 昨天 7/9 24:00 已结束（存 7/10 00:00），今天 7/10 → 过期
        int days = service.calcRemainingDays(
                LocalDateTime.of(2026, 7, 10, 0, 0), LocalDate.of(2026, 7, 10));
        assertEquals(-1, days);
    }

    @Test
    void shouldReturn5WhenFiveDaysAhead() {
        // 7/15 24:00 结束 → 存 7/16 00:00。今天 7/10 → 剩 5 天（7/11、7/12、7/13、7/14、7/15）
        int days = service.calcRemainingDays(
                LocalDateTime.of(2026, 7, 16, 0, 0), LocalDate.of(2026, 7, 10));
        assertEquals(5, days);
    }

    @Test
    void shouldReturn0WhenSameDayMorning() {
        // 7/10 0:01 开始计算 expireAt=7/11 00:00，今天 7/10，仍剩 0 天
        int days = service.calcRemainingDays(
                LocalDateTime.of(2026, 7, 11, 0, 0), LocalDate.of(2026, 7, 10));
        assertEquals(0, days);
    }
}