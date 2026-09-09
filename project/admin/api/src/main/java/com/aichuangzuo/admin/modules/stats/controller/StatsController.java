package com.aichuangzuo.admin.modules.stats.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.stats.service.StatsService;
import com.aichuangzuo.admin.modules.stats.vo.DashboardDistributionVO;
import com.aichuangzuo.admin.modules.stats.vo.DashboardOverviewVO;
import com.aichuangzuo.admin.modules.stats.vo.DashboardTrendVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端运营统计")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "查询运营概览看板")
    @GetMapping("/dashboard")
    public Result<DashboardOverviewVO> dashboard() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询运营看板, adminUserId={}", adminUserId);
        return Result.success(statsService.dashboard());
    }

    @Operation(summary = "查询运营走势（近 N 天每日数据）")
    @GetMapping("/dashboard/trend")
    public Result<DashboardTrendVO> trend(@RequestParam(defaultValue = "30") int days) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询运营走势, adminUserId={}, days={}", adminUserId, days);
        return Result.success(statsService.trend(days));
    }

    @Operation(summary = "查询看板分布数据（会员套餐/作品平台/收入套餐）")
    @GetMapping("/dashboard/distribution")
    public Result<DashboardDistributionVO> distribution() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询看板分布数据, adminUserId={}", adminUserId);
        return Result.success(statsService.distribution());
    }
}
