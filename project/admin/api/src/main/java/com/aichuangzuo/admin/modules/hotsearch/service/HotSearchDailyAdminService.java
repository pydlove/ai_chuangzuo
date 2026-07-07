package com.aichuangzuo.admin.modules.hotsearch.service;

import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchDailyQueryRequest;
import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchDailyRequest;
import com.aichuangzuo.admin.modules.hotsearch.vo.HotSearchDailyAdminVO;

import java.util.List;

public interface HotSearchDailyAdminService {
    PageResult list(HotSearchDailyQueryRequest request);
    HotSearchDailyAdminVO get(Long id);
    HotSearchDailyAdminVO create(HotSearchDailyRequest req);
    HotSearchDailyAdminVO update(Long id, HotSearchDailyRequest req);
    void delete(Long id);

    record PageResult(List<HotSearchDailyAdminVO> items, long total, long page, long size) {}
}
