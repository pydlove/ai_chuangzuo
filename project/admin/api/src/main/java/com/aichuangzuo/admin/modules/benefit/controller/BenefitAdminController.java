package com.aichuangzuo.admin.modules.benefit.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.benefit.entity.Benefit;
import com.aichuangzuo.admin.modules.benefit.service.BenefitAdminService;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端：读取套餐内容配置项定义。
 */
@Tag(name = "管理端-套餐配置项")
@RestController
@RequestMapping("/api/v1/admin/benefits")
@RequiredArgsConstructor
public class BenefitAdminController {

    private final BenefitAdminService benefitAdminService;
    private final AdminUserPermissionService adminUserPermissionService;

    @GetMapping
    public Result<List<Benefit>> list() {
        checkSuperAdmin();
        return Result.success(benefitAdminService.list());
    }

    private void checkSuperAdmin() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminUserId == null || !adminUserPermissionService.isSuperAdmin(adminUserId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
    }
}