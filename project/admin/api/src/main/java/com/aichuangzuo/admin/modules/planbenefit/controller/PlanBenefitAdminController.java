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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/plan-benefits")
@RequiredArgsConstructor
public class PlanBenefitAdminController {

    private final PlanBenefitAdminService planBenefitAdminService;
    private final AdminUserPermissionService adminUserPermissionService;

    @GetMapping
    public Result<List<PlanBenefit>> list() {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询套餐权益列表, adminUserId={}", adminUserId);
        return Result.success(planBenefitAdminService.list());
    }

    @PostMapping
    public Result<PlanBenefit> upsert(@Valid @RequestBody PlanBenefitUpsertRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员保存套餐权益, adminUserId={}, planKey={}, benefitCode={}",
                adminUserId, request.getPlanKey(), request.getBenefitCode());
        return Result.success(planBenefitAdminService.upsert(request, adminUserId));
    }

    private Long checkSuperAdmin() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminUserId == null || !adminUserPermissionService.isSuperAdmin(adminUserId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
        return adminUserId;
    }
}