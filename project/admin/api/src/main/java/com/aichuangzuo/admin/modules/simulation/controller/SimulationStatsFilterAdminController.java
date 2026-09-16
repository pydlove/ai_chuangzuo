package com.aichuangzuo.admin.modules.simulation.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.simulation.service.SimulationStatsFilterService;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "管理端-模拟运营统计过滤开关")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/simulation/stats-filter")
@RequiredArgsConstructor
public class SimulationStatsFilterAdminController {

    private final SimulationStatsFilterService filterService;
    private final AdminUserPermissionService permissionService;

    @GetMapping
    public Result<Map<String, Object>> get() {
        checkSuperAdmin();
        return Result.success(Map.of("includeRobots", filterService.includeRobots()));
    }

    @PutMapping
    public Result<Void> update(@RequestBody Map<String, Object> body) {
        Long adminId = checkSuperAdmin();
        boolean includeRobots = body.get("includeRobots") instanceof Boolean b && b;
        log.info("管理员更新模拟统计过滤开关, adminId={}, includeRobots={}", adminId, includeRobots);
        filterService.update(includeRobots);
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
