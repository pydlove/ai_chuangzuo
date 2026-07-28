package com.aichuangzuo.user.modules.commission.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.commission.dto.request.CommissionSubmitRequest;
import com.aichuangzuo.user.modules.commission.service.CommissionService;
import com.aichuangzuo.user.modules.commission.vo.CommissionSubmissionMineVO;
import com.aichuangzuo.user.modules.commission.vo.CommissionTaskDetailVO;
import com.aichuangzuo.user.modules.commission.vo.CommissionTaskVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户端约稿中心")
@RestController
@RequestMapping("/api/v1/user/commission")
@RequiredArgsConstructor
public class CommissionController {
    private final CommissionService commissionService;

    @Operation(summary = "约稿任务列表")
    @GetMapping("/tasks")
    public Result<IPage<CommissionTaskVO>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(commissionService.list(status, page, pageSize));
    }

    @Operation(summary = "约稿任务详情")
    @GetMapping("/tasks/{taskId}")
    public Result<CommissionTaskDetailVO> detail(@PathVariable Long taskId) {
        return Result.success(commissionService.detail(SecurityUserContext.getCurrentUserId(), taskId));
    }

    @Operation(summary = "投稿平台生成文章")
    @PostMapping("/tasks/{taskId}/submissions")
    public Result<Long> submit(@PathVariable Long taskId,
                               @Valid @RequestBody CommissionSubmitRequest request) {
        return Result.success(commissionService.submit(SecurityUserContext.getCurrentUserId(), taskId,
                request.getArticleBizNo()));
    }

    @Operation(summary = "撤回投稿")
    @DeleteMapping("/submissions/{submissionId}")
    public Result<Void> withdraw(@PathVariable Long submissionId) {
        commissionService.withdraw(SecurityUserContext.getCurrentUserId(), submissionId);
        return Result.success();
    }

    @Operation(summary = "我的投稿")
    @GetMapping("/submissions/mine")
    public Result<IPage<CommissionSubmissionMineVO>> mine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(commissionService.mySubmissions(SecurityUserContext.getCurrentUserId(), page, pageSize));
    }
}
