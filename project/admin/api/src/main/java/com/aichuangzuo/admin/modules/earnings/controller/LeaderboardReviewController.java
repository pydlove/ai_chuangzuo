package com.aichuangzuo.admin.modules.earnings.controller;

import com.aichuangzuo.admin.modules.earnings.dto.request.LeaderboardRejectRequest;
import com.aichuangzuo.admin.modules.earnings.service.LeaderboardReviewService;
import com.aichuangzuo.admin.modules.earnings.vo.IncomeSubmissionAdminVO;
import com.aichuangzuo.shared.result.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端自媒体审核")
@RestController
@RequestMapping("/api/v1/admin/leaderboards/income-submissions")
@RequiredArgsConstructor
public class LeaderboardReviewController {

    private final LeaderboardReviewService leaderboardReviewService;

    @Operation(summary = "申报列表")
    @GetMapping
    public Result<Page<IncomeSubmissionAdminVO>> list(
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "periodMonth", required = false) String periodMonth,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return Result.success(leaderboardReviewService.listSubmissions(status, periodMonth, page, size));
    }

    @Operation(summary = "通过")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        leaderboardReviewService.approve(id);
        return Result.success();
    }

    @Operation(summary = "拒绝")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @Valid @RequestBody LeaderboardRejectRequest request) {
        leaderboardReviewService.reject(id, request);
        return Result.success();
    }
}
