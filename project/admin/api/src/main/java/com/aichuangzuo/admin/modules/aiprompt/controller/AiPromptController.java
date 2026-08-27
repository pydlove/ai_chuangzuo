package com.aichuangzuo.admin.modules.aiprompt.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.aiprompt.dto.request.AiPromptCreateRequest;
import com.aichuangzuo.admin.modules.aiprompt.dto.request.AiPromptQueryRequest;
import com.aichuangzuo.admin.modules.aiprompt.dto.request.AiPromptTestRequest;
import com.aichuangzuo.admin.modules.aiprompt.dto.request.AiPromptUpdateRequest;
import com.aichuangzuo.admin.modules.aiprompt.service.AiPromptService;
import com.aichuangzuo.admin.modules.aiprompt.vo.AiPromptDetailVO;
import com.aichuangzuo.admin.modules.aiprompt.vo.AiPromptTestVO;
import com.aichuangzuo.admin.modules.aiprompt.vo.AiPromptVO;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
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

@Tag(name = "管理端 AI 提示词管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/ai-prompts")
@RequiredArgsConstructor
public class AiPromptController {

    private final AiPromptService aiPromptService;
    private final AdminUserPermissionService adminUserPermissionService;

    @Operation(summary = "查询提示词列表")
    @GetMapping
    public Result<AiPromptService.PageResult> list(@ModelAttribute AiPromptQueryRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询 AI 提示词列表, adminUserId={}", adminUserId);
        return Result.success(aiPromptService.list(request));
    }

    @Operation(summary = "查询分类列表")
    @GetMapping("/categories")
    public Result<List<String>> listCategories() {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询 AI 提示词分类列表, adminUserId={}", adminUserId);
        return Result.success(aiPromptService.listCategories());
    }

    @Operation(summary = "查看提示词详情")
    @GetMapping("/{id}")
    public Result<AiPromptDetailVO> get(@PathVariable("id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查看 AI 提示词详情, adminUserId={}, id={}", adminUserId, id);
        return Result.success(aiPromptService.get(id));
    }

    @Operation(summary = "新增提示词")
    @PostMapping
    public Result<Map<String, Long>> create(@Valid @RequestBody AiPromptCreateRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员新增 AI 提示词, adminUserId={}, promptCode={}", adminUserId, request.getPromptCode());
        Long id = aiPromptService.create(request);
        return Result.success(Map.of("id", id));
    }

    @Operation(summary = "编辑提示词")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody AiPromptUpdateRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员编辑 AI 提示词, adminUserId={}, id={}", adminUserId, id);
        aiPromptService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "启用提示词")
    @PostMapping("/{id}/actions/enable")
    public Result<Void> enable(@PathVariable("id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员启用 AI 提示词, adminUserId={}, id={}", adminUserId, id);
        aiPromptService.enable(id);
        return Result.success();
    }

    @Operation(summary = "停用提示词")
    @PostMapping("/{id}/actions/disable")
    public Result<Void> disable(@PathVariable("id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员停用 AI 提示词, adminUserId={}, id={}", adminUserId, id);
        aiPromptService.disable(id);
        return Result.success();
    }

    @Operation(summary = "测试提示词")
    @PostMapping("/{id}/actions/test")
    public Result<AiPromptTestVO> test(@PathVariable("id") Long id,
                                       @Valid @RequestBody AiPromptTestRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员测试 AI 提示词, adminUserId={}, id={}", adminUserId, id);
        return Result.success(aiPromptService.test(id, request.getVariables()));
    }

    private Long checkSuperAdmin() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminUserId == null || !adminUserPermissionService.isSuperAdmin(adminUserId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
        return adminUserId;
    }
}
