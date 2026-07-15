package com.aichuangzuo.admin.modules.generation.controller;

import com.aichuangzuo.admin.modules.generation.dto.request.GenerationTaskQueryRequest;
import com.aichuangzuo.admin.modules.generation.service.GenerationTaskAdminService;
import com.aichuangzuo.admin.modules.generation.vo.GenerationTaskAdminPageVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin 端-创作任务管理 API（队列页 + 运维操作）。
 */
@Tag(name = "管理端-创作队列")
@RestController
@RequestMapping("/api/v1/admin/generation/tasks")
@RequiredArgsConstructor
public class GenerationTaskAdminController {

    private final GenerationTaskAdminService service;

    /**
     * 任务列表（按 status 过滤 + 关键字 + 分页）。
     */
    @GetMapping
    public Result<GenerationTaskAdminPageVO> list(GenerationTaskQueryRequest request) {
        return Result.success(service.list(request));
    }

    /**
     * 手动停止任务：QUEUED / PROCESSING → FAILED。
     */
    @PostMapping("/{id}/stop")
    public Result<Void> stop(@PathVariable Long id) {
        service.stopTask(id);
        return Result.success();
    }

    /**
     * 手动标记失败。
     */
    @PostMapping("/{id}/mark-failed")
    public Result<Void> manualMarkFailed(@PathVariable Long id, @RequestBody MarkFailedRequest body) {
        service.manualMarkFailed(id, body == null ? null : body.getReason());
        return Result.success();
    }

    @Data
    public static class MarkFailedRequest {
        @NotNull
        @NotBlank
        private String reason;
    }
}
