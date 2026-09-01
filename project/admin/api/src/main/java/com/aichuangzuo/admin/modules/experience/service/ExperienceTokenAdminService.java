package com.aichuangzuo.admin.modules.experience.service;

import com.aichuangzuo.admin.modules.experience.dto.request.ExperienceTokenBatchGenerateRequest;
import com.aichuangzuo.admin.modules.experience.dto.request.ExperienceTokenQueryRequest;
import com.aichuangzuo.admin.modules.experience.vo.ExperienceTokenAdminVO;

import java.util.List;

public interface ExperienceTokenAdminService {

    List<String> batchGenerate(ExperienceTokenBatchGenerateRequest request, Long adminId);

    PageResult list(ExperienceTokenQueryRequest request);

    record PageResult(List<ExperienceTokenAdminVO> items, long total, long page, long size) {
    }
}
