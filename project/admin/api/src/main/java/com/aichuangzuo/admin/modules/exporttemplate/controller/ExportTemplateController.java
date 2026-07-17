package com.aichuangzuo.admin.modules.exporttemplate.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.exporttemplate.dto.request.ExportTemplateSaveRequest;
import com.aichuangzuo.admin.modules.exporttemplate.service.ExportTemplateService;
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

    private void checkSuperAdmin() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminUserId == null || !adminUserPermissionService.isSuperAdmin(adminUserId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
    }
}
