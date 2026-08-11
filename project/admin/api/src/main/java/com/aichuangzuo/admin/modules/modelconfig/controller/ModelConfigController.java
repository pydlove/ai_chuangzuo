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
    @GetMapping("/{providerType}")
    public Result<ModelConfigVO> get(@PathVariable(name = "providerType") String providerType) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查看模型配置详情, adminUserId={}, providerType={}", adminUserId, providerType);
        return Result.success(modelConfigService.getConfig(providerType));
    }

    @Operation(summary = "保存/更新模型配置")
    @PutMapping("/{providerType}")
    public Result<Void> save(@PathVariable(name = "providerType") String providerType,
                             @Valid @RequestBody ModelConfigSaveRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员保存模型配置, adminUserId={}, providerType={}, modelCode={}",
                adminUserId, providerType, request.getModelCode());
        modelConfigService.saveConfig(providerType, request);
        return Result.success();
    }

    @Operation(summary = "删除模型配置")
    @DeleteMapping("/{providerType}")
    public Result<Void> delete(@PathVariable(name = "providerType") String providerType) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员删除模型配置, adminUserId={}, providerType={}", adminUserId, providerType);
        modelConfigService.deleteConfig(providerType);
        return Result.success();
    }

    @Operation(summary = "拉取模型列表")
    @PostMapping("/{providerType}/actions/fetch-models")
    public Result<List<ModelOptionVO>> fetchModels(
            @PathVariable(name = "providerType") String providerType,
            @Valid @RequestBody ModelConfigConnectionRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员拉取厂商模型列表, adminUserId={}, providerType={}", adminUserId, providerType);
        return Result.success(modelConfigService.fetchModels(providerType, request));
    }

    @Operation(summary = "测试连接")
    @PostMapping("/{providerType}/actions/test-connection")
    public Result<Map<String, Boolean>> testConnection(
            @PathVariable(name = "providerType") String providerType,
            @Valid @RequestBody ModelConfigConnectionRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员测试模型连接, adminUserId={}, providerType={}", adminUserId, providerType);
        boolean success = modelConfigService.testConnection(providerType, request);
        return Result.success(Map.of("success", success));
    }

    @Operation(summary = "启用/停用配置")
    @PostMapping("/{providerType}/actions/toggle-active")
    public Result<Void> toggleActive(
            @PathVariable(name = "providerType") String providerType,
            @Valid @RequestBody ModelConfigActiveRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员切换模型配置状态, adminUserId={}, providerType={}, isActive={}",
                adminUserId, providerType, request.getIsActive());
        modelConfigService.toggleActive(providerType, request);
        return Result.success();
    }

    @Operation(summary = "问答测试：调用厂商 chat 接口并返回原始请求/响应")
    @PostMapping("/{providerType}/actions/chat-test")
    public Result<ModelConfigChatTestVO> chatTest(
            @PathVariable(name = "providerType") String providerType,
            @Valid @RequestBody ModelConfigChatTestRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员进行模型问答测试, adminUserId={}, providerType={}, modelCode={}",
                adminUserId, providerType, request.getModelCode());
        return Result.success(modelConfigService.chatTest(providerType, request));
    }

    private Long checkSuperAdmin() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminUserId == null || !adminUserPermissionService.isSuperAdmin(adminUserId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
        return adminUserId;
    }
}
