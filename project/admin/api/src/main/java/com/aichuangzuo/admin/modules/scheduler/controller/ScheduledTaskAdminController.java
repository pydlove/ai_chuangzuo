package com.aichuangzuo.admin.modules.scheduler.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.scheduler.service.ScheduledTaskService;
import com.aichuangzuo.admin.modules.scheduler.vo.ScheduledTaskLogVO;
import com.aichuangzuo.admin.modules.scheduler.vo.ScheduledTaskPageVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 定时任务管理接口。
 */
@Tag(name = "定时任务管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/scheduled-tasks")
@RequiredArgsConstructor
public class ScheduledTaskAdminController {

    private final ScheduledTaskService scheduledTaskService;

    @Operation(summary = "查询定时任务列表")
    @GetMapping
    public Result<ScheduledTaskPageVO> list(
            @RequestParam(name = "module", required = false) String module,
            @RequestParam(name = "page", defaultValue = "1") long page,
            @RequestParam(name = "pageSize", defaultValue = "20") long pageSize) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询定时任务列表, adminUserId={}, module={}, page={}, pageSize={}",
                adminUserId, module, page, pageSize);
        return Result.success(scheduledTaskService.list(module, page, pageSize));
    }

    @Operation(summary = "手动触发定时任务")
    @PostMapping("/{id}/actions/trigger")
    public Result<Map<String, Long>> trigger(@PathVariable("id") Long id) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员手动触发定时任务, adminUserId={}, taskId={}", adminUserId, id);
        Long logId = scheduledTaskService.trigger(id, adminUserId);
        return Result.success(Map.of("logId", logId));
    }

    @Operation(summary = "查询定时任务最近执行日志")
    @GetMapping("/{id}/logs")
    public Result<List<ScheduledTaskLogVO>> logs(
            @PathVariable("id") Long id,
            @RequestParam(name = "limit", defaultValue = "5") long limit) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询定时任务执行日志, adminUserId={}, taskId={}, limit={}", adminUserId, id, limit);
        return Result.success(scheduledTaskService.logs(id, limit));
    }
}
