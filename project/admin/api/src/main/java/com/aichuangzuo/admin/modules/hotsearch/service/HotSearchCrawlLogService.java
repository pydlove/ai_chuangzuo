package com.aichuangzuo.admin.modules.hotsearch.service;

import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchCrawlLogQueryRequest;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchCrawlLog;

import java.util.List;

public interface HotSearchCrawlLogService {

    PageResult list(HotSearchCrawlLogQueryRequest request);

    void saveLog(HotSearchCrawlLog log);

    record PageResult(List<HotSearchCrawlLog> items, long total, long page, long size) {}
}
