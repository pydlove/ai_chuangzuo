package com.aichuangzuo.admin.modules.leaderboard.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
// 自媒体收入榜功能暂时隐藏
// import com.aichuangzuo.admin.modules.leaderboard.dto.request.LeaderboardRejectRequest;
import com.aichuangzuo.admin.modules.leaderboard.dto.request.LeaderboardGrantRequest;
import com.aichuangzuo.admin.modules.leaderboard.service.LeaderboardAwardService;
// 自媒体收入榜功能暂时隐藏
// import com.aichuangzuo.admin.modules.leaderboard.service.LeaderboardReviewService;
// import com.aichuangzuo.admin.modules.leaderboard.vo.IncomeSubmissionAdminVO;
import com.aichuangzuo.admin.modules.leaderboard.vo.LeaderboardGrantResultVO;
import com.aichuangzuo.admin.modules.leaderboard.vo.LeaderboardTop10VO;
import com.aichuangzuo.admin.modules.leaderboard.vo.RewardRecordAdminVO;
import com.aichuangzuo.admin.modules.earnings.vo.PageResult;
import com.aichuangzuo.shared.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收益排行榜管理端接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/leaderboards")
@RequiredArgsConstructor
public class LeaderboardAdminController {

    // 自媒体收入榜功能暂时隐藏
    // private final LeaderboardReviewService reviewService;
    private final LeaderboardAwardService awardService;

    // 自媒体收入榜功能暂时隐藏
    // @GetMapping("/income-submissions")
    // public Result<IPage<IncomeSubmissionAdminVO>> page(@RequestParam(name = "status", required = false) Integer status,
    //                                                       @RequestParam(name = "page", defaultValue = "1") int page,
    //                                                       @RequestParam(name = "size", defaultValue = "20") int size) {
    //     return Result.success(reviewService.page(status, new Page<>(page, size)));
    // }

    // @PostMapping("/income-submissions/{id}/approve")
    // public Result<Void> approve(@PathVariable(name = "id") Long id) {
    //     reviewService.approve(id, currentAdminId());
    //     return Result.success();
    // }

    // @PostMapping("/income-submissions/{id}/reject")
    // public Result<Void> reject(@PathVariable(name = "id") Long id, @Valid @RequestBody LeaderboardRejectRequest request) {
    //     reviewService.reject(id, currentAdminId(), request.getReason());
    //     return Result.success();
    // }

    @GetMapping("/rewards/preview")
    public Result<List<LeaderboardTop10VO>> previewTop10(@RequestParam(name = "leaderboardType") Integer leaderboardType,
                                                         @RequestParam(name = "periodMonth") String periodMonth) {
        Long adminUserId = currentAdminId();
        log.info("管理员预览收益排行榜 Top10, adminUserId={}, leaderboardType={}, periodMonth={}",
                adminUserId, leaderboardType, periodMonth);
        return Result.success(awardService.previewTop10(leaderboardType, periodMonth));
    }

    @PostMapping("/rewards/actions/grant")
    public Result<LeaderboardGrantResultVO> grant(@Valid @RequestBody LeaderboardGrantRequest request) {
        Long adminUserId = currentAdminId();
        log.info("管理员发放收益排行榜奖励, adminUserId={}, leaderboardType={}, periodMonth={}",
                adminUserId, request.getLeaderboardType(), request.getPeriodMonth());
        return Result.success(awardService.grant(request.getLeaderboardType(), request.getPeriodMonth(), adminUserId));
    }

    @GetMapping("/rewards")
    public Result<PageResult<RewardRecordAdminVO>> rewards(@RequestParam(name = "leaderboardType", required = false) Integer leaderboardType,
                                                         @RequestParam(name = "periodMonth", required = false) String periodMonth,
                                                         @RequestParam(name = "page", defaultValue = "1") int page,
                                                         @RequestParam(name = "size", defaultValue = "20") int size) {
        Long adminUserId = currentAdminId();
        log.info("管理员查询收益排行榜发放记录, adminUserId={}, leaderboardType={}, periodMonth={}, page={}, size={}",
                adminUserId, leaderboardType, periodMonth, page, size);
        return Result.success(awardService.rewardHistory(leaderboardType, periodMonth, page, size));
    }

    private Long currentAdminId() {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        return adminId != null ? adminId : 0L;
    }
}
