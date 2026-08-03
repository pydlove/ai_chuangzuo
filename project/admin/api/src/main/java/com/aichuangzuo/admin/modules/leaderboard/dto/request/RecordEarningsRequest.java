package com.aichuangzuo.admin.modules.leaderboard.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 调用用户端内部记录收益接口的请求体。
 */
@Data
public class RecordEarningsRequest {

    private Long userId;
    private String type;
    private String sourceType;
    private String sourceId;
    private String title;
    private String description;
    private BigDecimal amount;
    private String settlementMonth;
}
