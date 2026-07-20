package com.aichuangzuo.admin.modules.plan.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.plan.dto.request.PlanUpsertRequest;
import com.aichuangzuo.admin.modules.plan.service.PlanAdminService;
import com.aichuangzuo.admin.modules.plan.vo.PlanVO;
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
 * 管理端：u_plan 维护（套餐价格/邀请奖励/推荐位/上下架）。
 */
@Tag(name = "管理端-套餐元数据")
@RestController
@RequestMapping("/api/v1/admin/plans")
@RequiredArgsConstructor
public class PlanAdminController {

    private final PlanAdminService planAdminService;
    private final AdminUserPermissionService adminUserPermissionService;

    @GetMapping
    public Result<List<PlanVO>> list() {
        checkSuperAdmin();
        return Result.success(planAdminService.list());
    }

    @PostMapping
    public Result<PlanVO> upsert(@Valid @RequestBody PlanUpsertRequest request) {
        checkSuperAdmin();
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        return Result.success(planAdminService.upsert(request, adminUserId));
    }

    private void checkSuperAdmin() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminUserId == null || !adminUserPermissionService.isSuperAdmin(adminUserId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
    }
}