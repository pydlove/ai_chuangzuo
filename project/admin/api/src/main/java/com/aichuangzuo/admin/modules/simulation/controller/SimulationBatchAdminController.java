package com.aichuangzuo.admin.modules.simulation.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.earnings.vo.PageResult;
import com.aichuangzuo.admin.modules.simulation.dto.request.SimulationBatchCreateRequest;
import com.aichuangzuo.admin.modules.simulation.dto.request.SimulationBatchQueryRequest;
import com.aichuangzuo.admin.modules.simulation.service.SimulationBatchService;
import com.aichuangzuo.admin.modules.simulation.vo.SimulationBatchDetailVO;
import com.aichuangzuo.admin.modules.simulation.vo.SimulationBatchVO;
import com.aichuangzuo.admin.modules.simulation.vo.SimulationRobotLogVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-模拟运营")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/simulation/batches")
@RequiredArgsConstructor
public class SimulationBatchAdminController {

    private final SimulationBatchService simulationBatchService;
    private final AdminUserPermissionService permissionService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody SimulationBatchCreateRequest request) {
        Long adminId = checkSuperAdmin();
        log.info("管理员创建模拟批次, adminId={}, userCount={}, planKey={}, cycle={}",
                adminId, request.getUserCount(), request.getPlanKey(), request.getCycle());
        return Result.success(simulationBatchService.create(request));
    }

    @GetMapping
    public Result<PageResult<SimulationBatchVO>> list(@ModelAttribute SimulationBatchQueryRequest request) {
        checkSuperAdmin();
        return Result.success(simulationBatchService.list(request));
    }

    @GetMapping("/{id}")
    public Result<SimulationBatchDetailVO> detail(@PathVariable Long id) {
        checkSuperAdmin();
        return Result.success(simulationBatchService.detail(id));
    }

    @GetMapping("/{id}/logs")
    public Result<PageResult<SimulationRobotLogVO>> logs(@PathVariable Long id,
                                                         @RequestParam(required = false) Long robotId,
                                                         @RequestParam(required = false) String stage,
                                                         @RequestParam(defaultValue = "1") Long page,
                                                         @RequestParam(defaultValue = "20") Long size) {
        checkSuperAdmin();
        return Result.success(simulationBatchService.logs(id, robotId, stage, page, size));
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        Long adminId = checkSuperAdmin();
        log.info("管理员取消模拟批次, adminId={}, batchId={}", adminId, id);
        simulationBatchService.cancel(id);
        return Result.success(null);
    }

    private Long checkSuperAdmin() {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminId == null || !permissionService.isSuperAdmin(adminId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
        return adminId;
    }
}
