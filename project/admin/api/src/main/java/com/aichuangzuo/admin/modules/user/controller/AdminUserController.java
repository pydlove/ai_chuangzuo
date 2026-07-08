package com.aichuangzuo.admin.modules.user.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.user.dto.request.AdminUserCreateRequest;
import com.aichuangzuo.admin.modules.user.dto.request.AdminUserMembershipRequest;
import com.aichuangzuo.admin.modules.user.dto.request.AdminUserStatusRequest;
import com.aichuangzuo.admin.modules.user.service.AdminUserService;
import com.aichuangzuo.admin.modules.user.vo.AdminUserOptionVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserPageVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserResetPasswordVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserVO;

import java.util.List;
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

    @Operation(summary = "手动创建用户")
    @PostMapping
    public Result<AdminUserVO> createUser(@Valid @RequestBody AdminUserCreateRequest request) {
        checkSuperAdmin();
        return Result.success(adminUserService.createUser(request));
    }

    @Operation(summary = "查看用户详情")
    @GetMapping("/{id}")
    public Result<AdminUserVO> getUser(@PathVariable(name = "id") Long id) {
        checkSuperAdmin();
        return Result.success(adminUserService.getUser(id));
    }

    @Operation(summary = "修改用户状态")
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable(name = "id") Long id,
                                     @Valid @RequestBody AdminUserStatusRequest request) {
        checkSuperAdmin();
        adminUserService.updateStatus(id, request);
        return Result.success();
    }

    @Operation(summary = "重置用户密码")
    @PostMapping("/{id}/reset-password")
    public Result<AdminUserResetPasswordVO> resetPassword(@PathVariable(name = "id") Long id) {
        checkSuperAdmin();
        return Result.success(adminUserService.resetPassword(id));
    }

    @Operation(summary = "设置会员到期时间（null=非会员）")
    @PatchMapping("/{id}/membership")
    public Result<Void> updateMembership(@PathVariable(name = "id") Long id,
                                          @RequestBody AdminUserMembershipRequest request) {
        checkSuperAdmin();
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        adminUserService.updateMembership(id, request.getExpireDate(), adminId);
        return Result.success();
    }

    @Operation(summary = "用户下拉选项（发布者选择）")
    @GetMapping("/options")
    public Result<List<AdminUserOptionVO>> listUserOptions(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return Result.success(adminUserService.listUserOptions(keyword, limit));
    }

    private void checkSuperAdmin() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminUserId == null || !adminUserPermissionService.isSuperAdmin(adminUserId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
    }
}
