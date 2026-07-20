package com.aichuangzuo.admin.modules.plan.service;

import com.aichuangzuo.admin.modules.plan.dto.request.PlanUpsertRequest;
import com.aichuangzuo.admin.modules.plan.vo.PlanVO;

import java.util.List;

public interface PlanAdminService {
    List<PlanVO> list();
    PlanVO upsert(PlanUpsertRequest request, Long adminUserId);
}