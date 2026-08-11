package com.aichuangzuo.admin.modules.lottery.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LotteryDrawRecordQueryRequest {

    private String drawType;
    private String email;
    private String nickname;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long page = 1L;
    private Long size = 20L;
}
