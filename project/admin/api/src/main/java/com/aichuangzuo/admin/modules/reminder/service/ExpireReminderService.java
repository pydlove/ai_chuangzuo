package com.aichuangzuo.admin.modules.reminder.service;

import com.aichuangzuo.admin.modules.reminder.dto.request.ExpiringUserPageQuery;
import com.aichuangzuo.admin.modules.reminder.vo.ExpiringUserVO;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpireReminderService {

    PageResult pageExpiringUsers(ExpiringUserPageQuery query);

    RemindResult remindUser(Long userId, String triggerType);

    /**
     * 计算到期日（次日 00:00 存储）→ 剩余天数（不含今天）。
     * 命中区间 0 ≤ remainingDays ≤ advanceDays。
     */
    int calcRemainingDays(LocalDateTime expireAt);

    record PageResult(List<ExpiringUserVO> items, long total, long page, long size) {}

    record RemindResult(Long userId, int remainingDays, List<String> sentChannels) {}
}