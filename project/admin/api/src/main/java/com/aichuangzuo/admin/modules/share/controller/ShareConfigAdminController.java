package com.aichuangzuo.admin.modules.share.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.share.dto.request.ShareConfigQueryRequest;
import com.aichuangzuo.admin.modules.share.dto.request.ShareConfigSaveRequest;
import com.aichuangzuo.admin.modules.share.service.ShareConfigAdminService;
import com.aichuangzuo.admin.modules.share.vo.ShareConfigAdminVO;
import com.aichuangzuo.shared.entity.ShareConfig;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端分享配置")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/share-config")
@RequiredArgsConstructor
public class ShareConfigAdminController {

    private final ShareConfigAdminService shareConfigAdminService;
    private final AdminUserPermissionService permissionService;

    @GetMapping
    public Result<ShareConfigAdminService.PageResult> list(@ModelAttribute ShareConfigQueryRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询分享配置列表, adminUserId={}, sceneKey={}, enabled={}",
                adminUserId, request.getSceneKey(), request.getEnabled());
        return Result.success(shareConfigAdminService.list(request));
    }

    @GetMapping("/{id}")
    public Result<ShareConfigAdminVO> get(@PathVariable("id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询分享配置详情, adminUserId={}, configId={}", adminUserId, id);
        return Result.success(shareConfigAdminService.get(id));
    }

    @PostMapping
    public Result<ShareConfig> create(@Valid @RequestBody ShareConfigSaveRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员创建分享配置, adminUserId={}, sceneKey={}", adminUserId, request.getSceneKey());
        return Result.success(shareConfigAdminService.create(request, adminUserId));
    }

    @PutMapping("/{id}")
    public Result<ShareConfig> update(@PathVariable("id") Long id, @Valid @RequestBody ShareConfigSaveRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员更新分享配置, adminUserId={}, configId={}", adminUserId, id);
        return Result.success(shareConfigAdminService.update(id, request, adminUserId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员删除分享配置, adminUserId={}, configId={}", adminUserId, id);
        shareConfigAdminService.delete(id, adminUserId);
        return Result.success();
    }

    private Long checkSuperAdmin() {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId == null || !permissionService.isSuperAdmin(adminId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
        return adminId;
    }
}
