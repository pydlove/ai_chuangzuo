package com.aichuangzuo.admin.modules.reminder.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.reminder.dto.request.ReminderConfigRequest;
import com.aichuangzuo.admin.modules.reminder.entity.ReminderConfig;
import com.aichuangzuo.admin.modules.reminder.service.ReminderConfigService;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端到期提醒")
@RestController
@RequestMapping("/api/v1/admin/expire-reminder")
@RequiredArgsConstructor
public class ReminderConfigController {

    private final ReminderConfigService configService;
    private final AdminUserPermissionService permissionService;

    @GetMapping("/config")
    public Result<ReminderConfig> getConfig() {
        checkSuperAdmin();
        return Result.success(configService.getConfig());
    }

    @PutMapping("/config")
    public Result<ReminderConfig> saveConfig(@Valid @RequestBody ReminderConfigRequest request) {
        checkSuperAdmin();
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        return Result.success(configService.saveConfig(request, adminId));
    }

    private void checkSuperAdmin() {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId == null || !permissionService.isSuperAdmin(adminId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
    }
}