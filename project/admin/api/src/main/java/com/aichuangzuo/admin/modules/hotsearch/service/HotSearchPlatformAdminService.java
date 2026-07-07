package com.aichuangzuo.admin.modules.hotsearch.service;

import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchPlatformRequest;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchPlatform;

import java.util.List;

public interface HotSearchPlatformAdminService {
    List<HotSearchPlatform> listAll();
    HotSearchPlatform create(HotSearchPlatformRequest req);
    HotSearchPlatform update(Long id, HotSearchPlatformRequest req);
    void delete(Long id);
}
