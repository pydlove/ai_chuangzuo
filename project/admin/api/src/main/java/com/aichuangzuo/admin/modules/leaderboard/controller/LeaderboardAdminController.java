package com.aichuangzuo.admin.modules.leaderboard.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.leaderboard.dto.request.LeaderboardGrantRequest;
import com.aichuangzuo.admin.modules.leaderboard.dto.request.LeaderboardRejectRequest;
import com.aichuangzuo.admin.modules.leaderboard.service.LeaderboardAwardService;
import com.aichuangzuo.admin.modules.leaderboard.service.LeaderboardReviewService;
import com.aichuangzuo.admin.modules.leaderboard.vo.IncomeSubmissionAdminVO;
import com.aichuangzuo.admin.modules.leaderboard.vo.LeaderboardGrantResultVO;
import com.aichuangzuo.admin.modules.leaderboard.vo.LeaderboardTop10VO;
import com.aichuangzuo.admin.modules.leaderboard.vo.RewardRecordAdminVO;
import com.aichuangzuo.shared.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收益排行榜管理端接口。
 */
@RestController
@RequestMapping("/api/v1/admin/leaderboards")
@RequiredArgsConstructor
public class LeaderboardAdminController {

    private final LeaderboardReviewService reviewService;
    private final LeaderboardAwardService awardService;

    @GetMapping("/income-submissions")
    public Result<IPage<IncomeSubmissionAdminVO>> page(@RequestParam(required = false) Integer status,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return Result.success(reviewService.page(status, new Page<>(page, size)));
    }

    @PostMapping("/income-submissions/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        reviewService.approve(id, currentAdminId());
        return Result.success();
    }

    @PostMapping("/income-submissions/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody LeaderboardRejectRequest request) {
        reviewService.reject(id, currentAdminId(), request.getReason());
        return Result.success();
    }

    @GetMapping("/rewards/preview")
    public Result<List<LeaderboardTop10VO>> previewTop10(@RequestParam Integer leaderboardType,
                                                         @RequestParam String periodMonth) {
        return Result.success(awardService.previewTop10(leaderboardType, periodMonth));
    }

    @PostMapping("/rewards/actions/grant")
    public Result<LeaderboardGrantResultVO> grant(@Valid @RequestBody LeaderboardGrantRequest request) {
        return Result.success(awardService.grant(request.getLeaderboardType(), request.getPeriodMonth(), currentAdminId()));
    }

    @GetMapping("/rewards")
    public Result<IPage<RewardRecordAdminVO>> rewards(@RequestParam(required = false) Integer leaderboardType,
                                                         @RequestParam(required = false) String periodMonth,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return Result.success(awardService.rewardHistory(leaderboardType, periodMonth, new Page<>(page, size)));
    }

    private Long currentAdminId() {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        return adminId != null ? adminId : 0L;
    }
}
