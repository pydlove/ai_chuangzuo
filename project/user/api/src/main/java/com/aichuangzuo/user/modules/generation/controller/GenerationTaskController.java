package com.aichuangzuo.user.modules.generation.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.generation.dto.request.GenerationRetryRequest;
import com.aichuangzuo.user.modules.generation.dto.request.GenerationSubmitRequest;
import com.aichuangzuo.user.modules.generation.service.GenerationTaskService;
import com.aichuangzuo.user.modules.generation.vo.GenerationTaskPageVO;
import com.aichuangzuo.user.modules.generation.vo.GenerationTaskVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端-创作任务 API（5 个端点）。
 */
@Tag(name = "用户端-创作任务")
@RestController
@RequestMapping("/api/v1/user/generation-tasks")
@RequiredArgsConstructor
public class GenerationTaskController {

    private final GenerationTaskService service;

    /** 提交创作任务。 */
    @PostMapping
    public Result<GenerationTaskVO> submit(@Valid @RequestBody GenerationSubmitRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(service.submit(request, userId));
    }

    /** 查进度。 */
    @GetMapping("/{id}")
    public Result<GenerationTaskVO> progress(@PathVariable Long id) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(service.getProgress(id, userId));
    }

    /** 重新生成（写新 task）。 */
    @PostMapping("/{id}/retry")
    public Result<GenerationTaskVO> retry(@PathVariable Long id,
                                          @RequestBody(required = false) GenerationRetryRequest req) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(service.retry(id, req, userId));
    }

    /** 我的任务列表。 */
    @GetMapping
    public Result<GenerationTaskPageVO> listMine(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "20") long pageSize) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(service.listMine(userId, page, pageSize));
    }
}
