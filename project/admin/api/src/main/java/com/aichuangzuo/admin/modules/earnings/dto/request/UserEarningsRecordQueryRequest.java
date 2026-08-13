package com.aichuangzuo.admin.modules.earnings.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserEarningsRecordQueryRequest {
    private String type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long page = 1L;
    private Long size = 20L;
}
