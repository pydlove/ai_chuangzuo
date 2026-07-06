package com.aichuangzuo.admin.modules.user.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.user.dto.request.AdminUserStatusRequest;
import com.aichuangzuo.admin.modules.user.service.AdminUserService;
import com.aichuangzuo.admin.modules.user.vo.AdminUserPageVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserResetPasswordVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端用户管理")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AdminUserPermissionService adminUserPermissionService;

    @Operation(summary = "查询用户列表")
    @GetMapping
    public Result<AdminUserPageVO> listUsers(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        checkSuperAdmin();
        return Result.success(adminUserService.listUsers(keyword, page, pageSize));
    }

    @Operation(summary = "查看用户详情")
    @GetMapping("/{id}")
    public Result<AdminUserVO> getUser(@PathVariable Long id) {
        checkSuperAdmin();
        return Result.success(adminUserService.getUser(id));
    }

    @Operation(summary = "修改用户状态")
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody AdminUserStatusRequest request) {
        checkSuperAdmin();
        adminUserService.updateStatus(id, request);
        return Result.success();
    }

    @Operation(summary = "重置用户密码")
    @PostMapping("/{id}/reset-password")
    public Result<AdminUserResetPasswordVO> resetPassword(@PathVariable Long id) {
        checkSuperAdmin();
        return Result.success(adminUserService.resetPassword(id));
    }

    private void checkSuperAdmin() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminUserId == null || !adminUserPermissionService.isSuperAdmin(adminUserId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
    }
}
