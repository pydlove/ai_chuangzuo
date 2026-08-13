package com.aichuangzuo.admin.modules.earnings.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCoinRecordQueryRequest {
    private Integer direction;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long page = 1L;
    private Long size = 20L;
}
