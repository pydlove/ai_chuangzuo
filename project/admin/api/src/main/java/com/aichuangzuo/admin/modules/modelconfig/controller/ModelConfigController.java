package com.aichuangzuo.admin.modules.modelconfig.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigActiveRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigConnectionRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigSaveRequest;
import com.aichuangzuo.admin.modules.modelconfig.service.ModelConfigService;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelConfigVO;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelOptionVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理端模型配置")
@RestController
@RequestMapping("/api/v1/admin/model-configs")
@RequiredArgsConstructor
public class ModelConfigController {

    private final ModelConfigService modelConfigService;
    private final AdminUserPermissionService adminUserPermissionService;

    @Operation(summary = "查询模型配置列表")
    @GetMapping
    public Result<List<ModelConfigVO>> list() {
        checkSuperAdmin();
        return Result.success(modelConfigService.listConfigs());
    }

    @Operation(summary = "查看模型配置详情")
    @GetMapping("/{providerType}")
    public Result<ModelConfigVO> get(@PathVariable(name = "providerType") String providerType) {
        checkSuperAdmin();
        return Result.success(modelConfigService.getConfig(providerType));
    }

    @Operation(summary = "保存/更新模型配置")
    @PutMapping("/{providerType}")
    public Result<Void> save(@PathVariable(name = "providerType") String providerType,
                             @Valid @RequestBody ModelConfigSaveRequest request) {
        checkSuperAdmin();
        modelConfigService.saveConfig(providerType, request);
        return Result.success();
    }

    @Operation(summary = "删除模型配置")
    @DeleteMapping("/{providerType}")
    public Result<Void> delete(@PathVariable(name = "providerType") String providerType) {
        checkSuperAdmin();
        modelConfigService.deleteConfig(providerType);
        return Result.success();
    }

    @Operation(summary = "拉取模型列表")
    @PostMapping("/{providerType}/actions/fetch-models")
    public Result<List<ModelOptionVO>> fetchModels(
            @PathVariable(name = "providerType") String providerType,
            @Valid @RequestBody ModelConfigConnectionRequest request) {
        checkSuperAdmin();
        return Result.success(modelConfigService.fetchModels(providerType, request));
    }

    @Operation(summary = "测试连接")
    @PostMapping("/{providerType}/actions/test-connection")
    public Result<Map<String, Boolean>> testConnection(
            @PathVariable(name = "providerType") String providerType,
            @Valid @RequestBody ModelConfigConnectionRequest request) {
        checkSuperAdmin();
        boolean success = modelConfigService.testConnection(providerType, request);
        return Result.success(Map.of("success", success));
    }

    @Operation(summary = "启用/停用配置")
    @PostMapping("/{providerType}/actions/toggle-active")
    public Result<Void> toggleActive(
            @PathVariable(name = "providerType") String providerType,
            @Valid @RequestBody ModelConfigActiveRequest request) {
        checkSuperAdmin();
        modelConfigService.toggleActive(providerType, request);
        return Result.success();
    }

    private void checkSuperAdmin() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminUserId == null || !adminUserPermissionService.isSuperAdmin(adminUserId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
    }
}
