package com.aichuangzuo.admin.modules.settings.upgrademanagement.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.settings.upgrademanagement.dto.request.UpgradeConfigUpdateRequest;
import com.aichuangzuo.admin.modules.settings.upgrademanagement.dto.request.UpgradeScriptExecuteRequest;
import com.aichuangzuo.admin.modules.settings.upgrademanagement.service.UpgradeManagementService;
import com.aichuangzuo.admin.modules.settings.upgrademanagement.service.UpgradeScriptExecutionService;
import com.aichuangzuo.admin.modules.settings.upgrademanagement.vo.UpgradeConfigVO;
import com.aichuangzuo.admin.modules.settings.upgrademanagement.vo.UpgradeJobLogVO;
import com.aichuangzuo.admin.modules.settings.upgrademanagement.vo.UpgradeScriptVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin 端 - 升级管理 API。
 */
@Tag(name = "管理端-升级管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/settings/upgrade-management")
@RequiredArgsConstructor
public class UpgradeManagementAdminController {

    private final UpgradeManagementService upgradeManagementService;
    private final UpgradeScriptExecutionService executionService;
    private final AdminUserPermissionService adminUserPermissionService;

    @Operation(summary = "查询升级配置")
    @GetMapping("/config")
    public Result<UpgradeConfigVO> detail() {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询升级配置, adminUserId={}", adminUserId);
        return Result.success(upgradeManagementService.detail());
    }

    @Operation(summary = "更新升级配置")
    @PutMapping("/config")
    public Result<UpgradeConfigVO> update(@Valid @RequestBody UpgradeConfigUpdateRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员更新升级配置, adminUserId={}, rootDir={}, serverIp={}",
                adminUserId, request.getScriptRootDir(), request.getServerIp());
        return Result.success(upgradeManagementService.update(request, adminUserId));
    }

    @Operation(summary = "列出脚本")
    @GetMapping("/scripts")
    public Result<List<UpgradeScriptVO>> listScripts() {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员列出升级脚本, adminUserId={}", adminUserId);
        return Result.success(upgradeManagementService.listScripts());
    }

    @Operation(summary = "执行脚本")
    @PostMapping("/actions/execute")
    public Result<Map<String, Long>> execute(@Valid @RequestBody UpgradeScriptExecuteRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员执行升级脚本, adminUserId={}, script={}, arguments={}",
                adminUserId, request.getScriptRelativePath(), request.getArguments());
        Long jobId = executionService.submit(request.getScriptRelativePath(), request.getArguments(), adminUserId);
        return Result.success(Map.of("jobId", jobId));
    }

    @Operation(summary = "分页查询执行记录")
    @GetMapping("/jobs")
    public Result<IPage<UpgradeJobLogVO>> listJobs(
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询升级执行记录, adminUserId={}, pageNum={}, pageSize={}", adminUserId, pageNum, pageSize);
        return Result.success(upgradeManagementService.listJobs(pageNum, pageSize));
    }

    @Operation(summary = "查看执行记录详情")
    @GetMapping("/jobs/{id}")
    public Result<UpgradeJobLogVO> getJob(@PathVariable(name = "id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查看升级执行记录, adminUserId={}, jobId={}", adminUserId, id);
        return Result.success(upgradeManagementService.getJob(id));
    }

    private Long checkSuperAdmin() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminUserId == null || !adminUserPermissionService.isSuperAdmin(adminUserId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
        return adminUserId;
    }
}
