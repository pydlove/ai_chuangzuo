package com.aichuangzuo.user.modules.scheduler.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.scheduler.entity.ScheduledTaskEntity;
import com.aichuangzuo.user.modules.scheduler.service.ScheduledTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 用户端定时任务内部接口，供管理端查询和触发。
 */
@RestController
@RequestMapping("/api/v1/user/internal/scheduled-tasks")
@RequiredArgsConstructor
@Slf4j
public class UserScheduledTaskInternalController {

    private final ScheduledTaskService scheduledTaskService;

    @GetMapping
    public Result<List<ScheduledTaskEntity>> list() {
        log.info("管理端查询用户端定时任务列表");
        return Result.success(scheduledTaskService.list());
    }

    @PostMapping("/{taskKey}/actions/trigger")
    public Result<Map<String, Long>> trigger(@PathVariable("taskKey") String taskKey,
                                            @RequestBody Map<String, Object> body) {
        Long adminId = body == null ? null : (body.get("adminId") instanceof Number n ? n.longValue() : null);
        log.info("管理端手动触发用户端定时任务, taskKey={}, adminId={}", taskKey, adminId);
        Long logId = scheduledTaskService.trigger(taskKey, adminId);
        return Result.success(Map.of("logId", logId));
    }
}
