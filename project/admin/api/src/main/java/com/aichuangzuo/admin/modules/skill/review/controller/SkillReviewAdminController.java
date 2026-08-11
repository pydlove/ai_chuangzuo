package com.aichuangzuo.admin.modules.skill.review.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.skill.review.dto.request.BatchApproveSkillReviewRequest;
import com.aichuangzuo.admin.modules.skill.review.dto.request.RejectSkillReviewRequest;
import com.aichuangzuo.admin.modules.skill.review.dto.request.SkillReviewPageRequest;
import com.aichuangzuo.admin.modules.skill.review.service.SkillReviewService;
import com.aichuangzuo.admin.modules.skill.review.vo.SkillReviewVO;
import com.aichuangzuo.shared.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 风格审核接口。
 */
@Tag(name = "管理端 - 风格审核")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/skill-reviews")
@RequiredArgsConstructor
public class SkillReviewAdminController {

    private final SkillReviewService skillReviewService;

    @Operation(summary = "审核列表（分页）")
    @GetMapping
    public Result<IPage<SkillReviewVO>> page(SkillReviewPageRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询提示词审核列表, adminUserId={}, status={}, reviewed={}, keyword={}, pageNum={}, pageSize={}",
                adminUserId, request.getStatus(), request.getReviewed(), request.getKeyword(),
                request.getPageNum(), request.getPageSize());
        return Result.success(skillReviewService.page(request));
    }

    @Operation(summary = "通过风格")
    @PostMapping("/{bizNo}/actions/approve")
    public Result<Void> approve(@PathVariable String bizNo) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员通过提示词审核, adminUserId={}, bizNo={}", adminUserId, bizNo);
        skillReviewService.approve(bizNo);
        return Result.success();
    }

    @Operation(summary = "批量通过风格")
    @PostMapping("/actions/batch-approve")
    public Result<Integer> batchApprove(@Valid @RequestBody BatchApproveSkillReviewRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员批量通过提示词审核, adminUserId={}, bizNoCount={}",
                adminUserId, request.getBizNos().size());
        int count = skillReviewService.batchApprove(request.getBizNos());
        return Result.success(count);
    }

    @Operation(summary = "打回风格")
    @PostMapping("/{bizNo}/actions/reject")
    public Result<Void> reject(@PathVariable String bizNo,
                                @Valid @RequestBody RejectSkillReviewRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员打回提示词审核, adminUserId={}, bizNo={}, reason={}",
                adminUserId, bizNo, request.getReason());
        skillReviewService.reject(bizNo, request.getReason());
        return Result.success();
    }
}