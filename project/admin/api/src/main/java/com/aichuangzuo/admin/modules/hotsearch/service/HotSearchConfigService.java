package com.aichuangzuo.admin.modules.hotsearch.service;

import com.aichuangzuo.admin.modules.hotsearch.dto.request.HotSearchConfigRequest;
import com.aichuangzuo.admin.modules.hotsearch.entity.HotSearchConfig;

public interface HotSearchConfigService {
    HotSearchConfig getConfig();

    HotSearchConfig saveConfig(HotSearchConfigRequest request, Long updatedBy);

    void syncFromProperties();
}
