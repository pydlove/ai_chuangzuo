package com.aichuangzuo.admin.modules.reminder.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExpiringUserVO {
    private Long userId;
    private String email;
    private String nickname;
    private LocalDateTime membershipExpireAt;
    private Integer remainingDays;
    private LocalDateTime lastRemindedAt;
    private String lastReminderChannel;
}