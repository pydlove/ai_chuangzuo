package com.aichuangzuo.admin.modules.hotsearch.dto.request;

import lombok.Data;

@Data
public class HotSearchCrawlLogQueryRequest {
    private String triggerType;
    private Long page = 1L;
    private Long size = 20L;
}
