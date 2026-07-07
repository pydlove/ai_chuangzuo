package com.aichuangzuo.admin.modules.hotsearch.vo;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class CrawlResultVO {
    private List<PlatformCrawlResultVO> results;
    private Instant startedAt;
    private Instant finishedAt;
}
