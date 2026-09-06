package com.aichuangzuo.admin.modules.stats.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.stats.service.StatsService;
import com.aichuangzuo.admin.modules.stats.vo.StatsOverviewVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端运营统计")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "查询当前在线人数和今日日活")
    @GetMapping("/overview")
    public Result<StatsOverviewVO> overview() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询运营概览, adminUserId={}", adminUserId);
        return Result.success(statsService.overview());
    }
}
