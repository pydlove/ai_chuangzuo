package com.aichuangzuo.admin.modules.reminder.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.reminder.dto.request.ExpiringUserPageQuery;
import com.aichuangzuo.admin.modules.reminder.service.ExpireReminderService;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端到期提醒-用户列表与手动提醒")
@RestController
@RequestMapping("/api/v1/admin/expire-reminder")
@RequiredArgsConstructor
public class ExpireReminderController {

    private final ExpireReminderService reminderService;
    private final AdminUserPermissionService permissionService;

    @GetMapping("/users")
    public Result<ExpireReminderService.PageResult> listExpiringUsers(
            @RequestParam(name = "advanceDays", required = false) Integer advanceDays,
            @RequestParam(name = "page", defaultValue = "1") Long page,
            @RequestParam(name = "pageSize", defaultValue = "20") Long pageSize) {
        checkSuperAdmin();
        ExpiringUserPageQuery query = new ExpiringUserPageQuery();
        query.setAdvanceDays(advanceDays);
        query.setPage(page);
        query.setSize(pageSize);
        return Result.success(reminderService.pageExpiringUsers(query));
    }

    @PostMapping("/users/{userId}/remind")
    public Result<ExpireReminderService.RemindResult> remind(@PathVariable("userId") Long userId) {
        checkSuperAdmin();
        return Result.success(reminderService.remindUser(userId, "manual"));
    }

    private void checkSuperAdmin() {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId == null || !permissionService.isSuperAdmin(adminId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
    }
}
