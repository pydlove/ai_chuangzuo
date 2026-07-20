package com.aichuangzuo.admin.modules.planbenefit.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.planbenefit.dto.request.PlanBenefitUpsertRequest;
import com.aichuangzuo.admin.modules.planbenefit.entity.PlanBenefit;
import com.aichuangzuo.admin.modules.planbenefit.service.PlanBenefitAdminService;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端：u_plan_benefit 维护（套餐 × 权益矩阵的值）。
 */
@Tag(name = "管理端-套餐权益值")
@RestController
@RequestMapping("/api/v1/admin/plan-benefits")
@RequiredArgsConstructor
public class PlanBenefitAdminController {

    private final PlanBenefitAdminService planBenefitAdminService;
    private final AdminUserPermissionService adminUserPermissionService;

    @GetMapping
    public Result<List<PlanBenefit>> list() {
        checkSuperAdmin();
        return Result.success(planBenefitAdminService.list());
    }

    @PostMapping
    public Result<PlanBenefit> upsert(@Valid @RequestBody PlanBenefitUpsertRequest request) {
        checkSuperAdmin();
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        return Result.success(planBenefitAdminService.upsert(request, adminUserId));
    }

    private void checkSuperAdmin() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminUserId == null || !adminUserPermissionService.isSuperAdmin(adminUserId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
    }
}