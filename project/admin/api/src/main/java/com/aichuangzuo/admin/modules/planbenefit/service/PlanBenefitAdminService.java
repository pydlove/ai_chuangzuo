package com.aichuangzuo.admin.modules.planbenefit.service;

import com.aichuangzuo.admin.modules.planbenefit.dto.request.PlanBenefitUpsertRequest;
import com.aichuangzuo.admin.modules.planbenefit.entity.PlanBenefit;

import java.util.List;

public interface PlanBenefitAdminService {
    List<PlanBenefit> list();
    PlanBenefit upsert(PlanBenefitUpsertRequest request, Long adminUserId);
}