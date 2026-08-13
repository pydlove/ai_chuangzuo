package com.aichuangzuo.admin.modules.modelconfig.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigActiveRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigChatTestRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigConnectionRequest;
import com.aichuangzuo.admin.modules.modelconfig.dto.request.ModelConfigSaveRequest;
import com.aichuangzuo.admin.modules.modelconfig.service.ModelConfigService;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelConfigChatTestVO;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelConfigVO;
import com.aichuangzuo.admin.modules.modelconfig.vo.ModelOptionVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理端模型配置")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/model-configs")
@RequiredArgsConstructor
public class ModelConfigController {

    private final ModelConfigService modelConfigService;
    private final AdminUserPermissionService adminUserPermissionService;

    @Operation(summary = "查询模型配置列表")
    @GetMapping
    public Result<List<ModelConfigVO>> list() {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询模型配置列表, adminUserId={}", adminUserId);
        return Result.success(modelConfigService.listConfigs());
    }

    @Operation(summary = "查看模型配置详情")
    @GetMapping("/{id}")
    public Result<ModelConfigVO> get(@PathVariable(name = "id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查看模型配置详情, adminUserId={}, id={}", adminUserId, id);
        return Result.success(modelConfigService.getConfig(id));
    }

    @Operation(summary = "创建模型配置")
    @PostMapping
    public Result<Map<String, Long>> create(@Valid @RequestBody ModelConfigSaveRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员创建模型配置, adminUserId={}, providerType={}, name={}",
                adminUserId, request.getProviderType(), request.getName());
        Long id = modelConfigService.createConfig(request);
        return Result.success(Map.of("id", id));
    }

    @Operation(summary = "更新模型配置")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable(name = "id") Long id,
                               @Valid @RequestBody ModelConfigSaveRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员更新模型配置, adminUserId={}, id={}, providerType={}, name={}",
                adminUserId, id, request.getProviderType(), request.getName());
        modelConfigService.updateConfig(id, request);
        return Result.success();
    }

    @Operation(summary = "删除模型配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable(name = "id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员删除模型配置, adminUserId={}, id={}", adminUserId, id);
        modelConfigService.deleteConfig(id);
        return Result.success();
    }

    @Operation(summary = "拉取模型列表")
    @PostMapping("/actions/fetch-models")
    public Result<List<ModelOptionVO>> fetchModels(
            @Valid @RequestBody ModelConfigConnectionRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员拉取厂商模型列表, adminUserId={}, providerType={}", adminUserId, request.getProviderType());
        return Result.success(modelConfigService.fetchModels(request));
    }

    @Operation(summary = "查询厂商已持久化的模型列表")
    @GetMapping("/provider-models")
    public Result<List<ModelOptionVO>> listProviderModels(
            @RequestParam(name = "providerType") String providerType) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询厂商模型列表, adminUserId={}, providerType={}", adminUserId, providerType);
        return Result.success(modelConfigService.listProviderModels(providerType));
    }

    @Operation(summary = "测试连接")
    @PostMapping("/actions/test-connection")
    public Result<Map<String, Boolean>> testConnection(
            @Valid @RequestBody ModelConfigConnectionRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员测试模型连接, adminUserId={}, providerType={}, name={}",
                adminUserId, request.getProviderType(), request.getBaseUrl());
        boolean success = modelConfigService.testConnection(request);
        return Result.success(Map.of("success", success));
    }

    @Operation(summary = "启用/停用配置")
    @PostMapping("/{id}/actions/toggle-active")
    public Result<Void> toggleActive(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody ModelConfigActiveRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员切换模型配置状态, adminUserId={}, id={}, isActive={}",
                adminUserId, id, request.getIsActive());
        modelConfigService.toggleActive(id, request);
        return Result.success();
    }

    @Operation(summary = "问答测试：调用厂商 chat 接口并返回原始请求/响应")
    @PostMapping("/actions/chat-test")
    public Result<ModelConfigChatTestVO> chatTest(
            @Valid @RequestBody ModelConfigChatTestRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员进行模型问答测试, adminUserId={}, providerType={}, modelCode={}",
                adminUserId, request.getProviderType(), request.getModelCode());
        return Result.success(modelConfigService.chatTest(request));
    }

    private Long checkSuperAdmin() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminUserId == null || !adminUserPermissionService.isSuperAdmin(adminUserId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
        return adminUserId;
    }
}
