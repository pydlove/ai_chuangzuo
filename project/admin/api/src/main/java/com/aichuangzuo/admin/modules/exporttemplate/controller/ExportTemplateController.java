package com.aichuangzuo.admin.modules.exporttemplate.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.exporttemplate.dto.request.ExportTemplateParamSaveRequest;
import com.aichuangzuo.admin.modules.exporttemplate.dto.request.ExportTemplateSaveRequest;
import com.aichuangzuo.admin.modules.exporttemplate.service.ExportTemplateService;
import com.aichuangzuo.admin.modules.exporttemplate.vo.ExportTemplateParamVO;
import com.aichuangzuo.admin.modules.exporttemplate.vo.ExportTemplateVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理端导出模板")
@RestController
@RequestMapping("/api/v1/admin/export-templates")
@RequiredArgsConstructor
public class ExportTemplateController {

    private final ExportTemplateService exportTemplateService;
    private final AdminUserPermissionService adminUserPermissionService;

    // ===== 模板 CRUD =====

    @Operation(summary = "查询模板列表")
    @GetMapping
    public Result<List<ExportTemplateVO>> list() {
        checkSuperAdmin();
        return Result.success(exportTemplateService.listAll());
    }

    @Operation(summary = "查看模板详情")
    @GetMapping("/{id}")
    public Result<ExportTemplateVO> get(@PathVariable Long id) {
        checkSuperAdmin();
        return Result.success(exportTemplateService.getById(id));
    }

    @Operation(summary = "新增模板")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody ExportTemplateSaveRequest request) {
        checkSuperAdmin();
        exportTemplateService.save(request);
        return Result.success();
    }

    @Operation(summary = "更新模板")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody ExportTemplateSaveRequest request) {
        checkSuperAdmin();
        exportTemplateService.update(id, request);
        return Result.success();
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkSuperAdmin();
        exportTemplateService.delete(id);
        return Result.success();
    }

    // ===== 参数定义 CRUD =====

    @Operation(summary = "查询参数定义列表")
    @GetMapping("/params")
    public Result<List<ExportTemplateParamVO>> listParams() {
        checkSuperAdmin();
        return Result.success(exportTemplateService.listParams());
    }

    @Operation(summary = "新增参数定义")
    @PostMapping("/params")
    public Result<Void> saveParam(@Valid @RequestBody ExportTemplateParamSaveRequest request) {
        checkSuperAdmin();
        exportTemplateService.saveParam(request);
        return Result.success();
    }

    @Operation(summary = "更新参数定义")
    @PutMapping("/params/{id}")
    public Result<Void> updateParam(@PathVariable Long id,
                                    @Valid @RequestBody ExportTemplateParamSaveRequest request) {
        checkSuperAdmin();
        exportTemplateService.updateParam(id, request);
        return Result.success();
    }

    @Operation(summary = "删除参数定义")
    @DeleteMapping("/params/{id}")
    public Result<Void> deleteParam(@PathVariable Long id) {
        checkSuperAdmin();
        exportTemplateService.deleteParam(id);
        return Result.success();
    }

    private void checkSuperAdmin() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminUserId == null || !adminUserPermissionService.isSuperAdmin(adminUserId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
    }
}