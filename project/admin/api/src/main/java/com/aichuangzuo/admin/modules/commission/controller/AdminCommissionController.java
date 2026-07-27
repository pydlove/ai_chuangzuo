package com.aichuangzuo.admin.modules.commission.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.commission.dto.request.CommissionAdoptRequest;
import com.aichuangzuo.admin.modules.commission.dto.request.CommissionTaskCreateRequest;
import com.aichuangzuo.admin.modules.commission.dto.request.CommissionTaskUpdateRequest;
import com.aichuangzuo.admin.modules.commission.entity.CommissionTask;
import com.aichuangzuo.admin.modules.commission.service.AdminCommissionService;
import com.aichuangzuo.admin.modules.commission.vo.CommissionTaskDetailVO;
import com.aichuangzuo.shared.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端约稿中心")
@RestController
@RequestMapping("/api/v1/admin/commission/tasks")
@RequiredArgsConstructor
public class AdminCommissionController {
    private final AdminCommissionService commissionService;

    @Operation(summary = "约稿任务列表")
    @GetMapping
    public Result<IPage<CommissionTask>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(commissionService.list(keyword, status, page, pageSize));
    }

    @Operation(summary = "约稿任务详情及投稿列表")
    @GetMapping("/{taskId}")
    public Result<CommissionTaskDetailVO> detail(@PathVariable Long taskId) {
        return Result.success(commissionService.detail(taskId));
    }

    @Operation(summary = "发布约稿任务")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CommissionTaskCreateRequest request) {
        return Result.success(commissionService.create(request, SecurityAdminContext.getCurrentAdminUserId()));
    }

    @Operation(summary = "编辑约稿任务（仅招募中可编辑）")
    @PutMapping("/{taskId}")
    public Result<Void> update(@PathVariable Long taskId, @Valid @RequestBody CommissionTaskUpdateRequest request) {
        commissionService.update(taskId, request);
        return Result.success();
    }

    @Operation(summary = "结束投递（投递期 → 评选期）")
    @PostMapping("/{taskId}/close")
    public Result<Void> close(@PathVariable Long taskId) {
        commissionService.close(taskId);
        return Result.success();
    }

    @Operation(summary = "批量采纳投稿并发放奖励")
    @PostMapping("/{taskId}/adopt")
    public Result<Void> adopt(@PathVariable Long taskId,
                              @Valid @RequestBody CommissionAdoptRequest request) {
        commissionService.adopt(taskId, request.getSubmissionIds());
        return Result.success();
    }
}
