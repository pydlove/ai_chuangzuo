package com.aichuangzuo.admin.modules.reminder.dto.request;

import lombok.Data;

@Data
public class ExpiringUserPageQuery {
    private Integer advanceDays;
    private Long page = 1L;
    private Long size = 20L;
}