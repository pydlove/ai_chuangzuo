package com.aichuangzuo.admin.modules.share.service;

import com.aichuangzuo.admin.modules.share.dto.request.ShareConfigQueryRequest;
import com.aichuangzuo.admin.modules.share.dto.request.ShareConfigSaveRequest;
import com.aichuangzuo.admin.modules.share.vo.ShareConfigAdminVO;
import com.aichuangzuo.shared.entity.ShareConfig;

import java.util.List;

public interface ShareConfigAdminService {

    PageResult list(ShareConfigQueryRequest request);

    ShareConfigAdminVO get(Long id);

    ShareConfig create(ShareConfigSaveRequest request, Long adminId);

    ShareConfig update(Long id, ShareConfigSaveRequest request, Long adminId);

    void delete(Long id, Long adminId);

    record PageResult(List<ShareConfigAdminVO> items, long total, long page, long size) {
    }
}
