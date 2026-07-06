package com.aichuangzuo.admin.modules.hotsearch.controller;

import com.aichuangzuo.admin.modules.hotsearch.job.HotSearchCrawlJob;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 热搜手动触发抓取接口（仅测试/开发使用）。
 */
@Tag(name = "热搜数据管理")
@RestController
@RequestMapping("/api/v1/admin/hot-search")
@RequiredArgsConstructor
public class HotSearchCrawlController {

    private final HotSearchCrawlJob crawlJob;

    @Operation(summary = "手动触发热搜抓取")
    @PostMapping("/crawl")
    public Result<Void> crawl() {
        crawlJob.crawl();
        return Result.success();
    }
}
