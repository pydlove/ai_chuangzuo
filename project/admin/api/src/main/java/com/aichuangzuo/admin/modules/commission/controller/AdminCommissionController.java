package com.aichuangzuo.admin.modules.commission.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.commission.dto.request.CommissionAdoptRequest;
import com.aichuangzuo.admin.modules.commission.dto.request.CommissionSubmissionBatchCreateRequest;
import com.aichuangzuo.admin.modules.commission.dto.request.CommissionSubmissionCreateRequest;
import com.aichuangzuo.admin.modules.commission.dto.request.CommissionTaskCreateRequest;
import com.aichuangzuo.admin.modules.commission.dto.request.CommissionTaskBatchDeleteRequest;
import com.aichuangzuo.admin.modules.commission.dto.request.CommissionTaskUpdateRequest;
import com.aichuangzuo.admin.modules.commission.service.AdminCommissionService;
import com.aichuangzuo.admin.modules.commission.vo.CommissionTaskDetailVO;
import com.aichuangzuo.admin.modules.commission.vo.CommissionTaskImportResultVO;
import com.aichuangzuo.admin.modules.commission.vo.CommissionTaskListVO;
import com.aichuangzuo.shared.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "管理端约稿中心")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/commission/tasks")
@RequiredArgsConstructor
public class AdminCommissionController {
    private final AdminCommissionService commissionService;

    @Operation(summary = "约稿任务列表")
    @GetMapping
    public Result<IPage<CommissionTaskListVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询约稿任务列表, adminUserId={}, keyword={}, status={}, page={}, pageSize={}",
                adminUserId, keyword, status, page, pageSize);
        return Result.success(commissionService.list(keyword, status, page, pageSize));
    }

    @Operation(summary = "约稿任务详情及投稿列表")
    @GetMapping("/{taskId}")
    public Result<CommissionTaskDetailVO> detail(@PathVariable Long taskId) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询约稿任务详情, adminUserId={}, taskId={}", adminUserId, taskId);
        return Result.success(commissionService.detail(taskId));
    }

    @Operation(summary = "发布约稿任务")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CommissionTaskCreateRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员发布约稿任务, adminUserId={}, title={}, rewardCoin={}, neededCount={}",
                adminUserId, request.getTitle(), request.getRewardCoin(), request.getNeededCount());
        return Result.success(commissionService.create(request, adminUserId));
    }

    @Operation(summary = "编辑约稿任务（仅招募中可编辑）")
    @PutMapping("/{taskId}")
    public Result<Void> update(@PathVariable Long taskId, @Valid @RequestBody CommissionTaskUpdateRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员编辑约稿任务, adminUserId={}, taskId={}, title={}",
                adminUserId, taskId, request.getTitle());
        commissionService.update(taskId, request);
        return Result.success();
    }

    @Operation(summary = "结束投递（投递期 → 评选期）")
    @PostMapping("/{taskId}/close")
    public Result<Void> close(@PathVariable Long taskId) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员结束约稿任务投递, adminUserId={}, taskId={}", adminUserId, taskId);
        commissionService.close(taskId);
        return Result.success();
    }

    @Operation(summary = "批量采纳投稿并发放奖励")
    @PostMapping("/{taskId}/adopt")
    public Result<Void> adopt(@PathVariable Long taskId,
                              @Valid @RequestBody CommissionAdoptRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员批量采纳约稿投稿, adminUserId={}, taskId={}, submissionCount={}",
                adminUserId, taskId, request.getSubmissionIds().size());
        commissionService.adopt(taskId, request.getSubmissionIds());
        return Result.success();
    }

    @Operation(summary = "手动校正约稿任务状态（返回变更条数）")
    @PostMapping("/reconcile")
    public Result<Integer> reconcile() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员手动校正约稿任务状态, adminUserId={}", adminUserId);
        return Result.success(commissionService.reconcileTaskStatus());
    }

    @Operation(summary = "手动为用户添加投稿（运营机器人/代投）")
    @PostMapping("/{taskId}/submissions")
    public Result<Long> createSubmission(@PathVariable Long taskId,
                                         @Valid @RequestBody CommissionSubmissionCreateRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员手动添加约稿投稿, adminUserId={}, taskId={}, submitterId={}, articleTitle={}",
                adminUserId, taskId, request.getSubmitterId(), request.getArticleTitle());
        return Result.success(commissionService.createSubmission(taskId, request, adminUserId));
    }

    @Operation(summary = "批量手动添加投稿人")
    @PostMapping("/{taskId}/submissions/batch")
    public Result<Integer> createSubmissionBatch(@PathVariable Long taskId,
                                                 @Valid @RequestBody CommissionSubmissionBatchCreateRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员批量手动添加约稿投稿人, adminUserId={}, taskId={}, submitterCount={}",
                adminUserId, taskId, request.getSubmitterIds().size());
        return Result.success(commissionService.createSubmissionBatch(taskId, request, adminUserId));
    }



    @Operation(summary = "批量删除约稿任务")
    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@Valid @RequestBody CommissionTaskBatchDeleteRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员批量删除约稿任务, adminUserId={}, ids={}", adminUserId, request.getIds());
        commissionService.batchDelete(request.getIds());
        return Result.success();
    }
    @Operation(summary = "从Excel批量导入约稿任务")
    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<CommissionTaskImportResultVO> importExcel(@RequestParam("file") MultipartFile file) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员批量导入约稿任务, adminUserId={}, fileName={}, fileSize={}",
                adminUserId, file.getOriginalFilename(), file.getSize());
        return Result.success(commissionService.importExcel(file, adminUserId));
    }
}
