package com.aichuangzuo.admin.modules.generation.controller;

import com.aichuangzuo.admin.modules.generation.service.GenerationCallLogService;
import com.aichuangzuo.admin.modules.generation.vo.GenerationCallLogGroupedVO;
import com.aichuangzuo.admin.modules.generation.vo.GenerationCallLogVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin 端-创作 AI 调用日志查询 API。
 */
@Tag(name = "管理端-创作调用日志")
@RestController
@RequestMapping("/api/v1/admin/generation/call-logs")
@RequiredArgsConstructor
public class GenerationCallLogController {

    private final GenerationCallLogService service;

    /**
     * 查某任务的所有 AI 调用记录（按 stage 升序）。
     */
    @GetMapping("/by-task/{taskId}")
    public Result<List<GenerationCallLogVO>> listByTask(@PathVariable Long taskId) {
        return Result.success(service.queryByTaskId(taskId));
    }

    /**
     * 查某任务的全部调用日志（按 stage 分组），并附带任务当前 status。
     * 适合前端「执行过程」抽屉轮询使用：taskStatus=2 时停止刷新。
     */
    @GetMapping("/by-task/{taskId}/grouped")
    public Result<GenerationCallLogGroupedVO> listByTaskGrouped(@PathVariable Long taskId) {
        return Result.success(service.queryByTaskIdGrouped(taskId));
    }
}