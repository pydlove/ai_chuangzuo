package com.aichuangzuo.admin.modules.hotsearch.vo;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class LastRunVO {
    private Instant lastRunAt;
    private int totalFetched;
    private int successCount;
    private int failCount;
    private List<PlatformCrawlResultVO> results;
}
