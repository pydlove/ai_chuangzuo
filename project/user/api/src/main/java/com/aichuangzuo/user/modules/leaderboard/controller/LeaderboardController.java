package com.aichuangzuo.user.modules.leaderboard.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.leaderboard.dto.request.IncomeSubmissionUploadRequest;
import com.aichuangzuo.user.modules.leaderboard.service.IncomeSubmissionService;
import com.aichuangzuo.user.modules.leaderboard.service.LeaderboardService;
import com.aichuangzuo.user.modules.leaderboard.vo.CoinLeaderboardVO;
import com.aichuangzuo.user.modules.leaderboard.vo.IncomeLeaderboardVO;
import com.aichuangzuo.user.modules.leaderboard.vo.IncomeSubmissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * 收益排行榜用户端接口。
 */
@RestController
@RequestMapping("/api/v1/user/leaderboards")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final IncomeSubmissionService incomeSubmissionService;

    @GetMapping("/coin")
    public Result<CoinLeaderboardVO> coin(@RequestParam(name = "month") String month) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(leaderboardService.getCoinLeaderboard(userId, month));
    }

    @GetMapping("/income")
    public Result<IncomeLeaderboardVO> income(@RequestParam(name = "periodType") String periodType,
                                                 @RequestParam(name = "periodValue") String periodValue) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(leaderboardService.getIncomeLeaderboard(userId, periodType, periodValue));
    }

    @PostMapping(value = "/income-submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<IncomeSubmissionVO> submit(@RequestParam(name = "periodMonth") String periodMonth,
                                              @RequestParam(name = "amount") BigDecimal amount,
                                              @RequestParam(name = "platform") String platform,
                                              @RequestParam("screenshots") List<MultipartFile> screenshots) {
        Long userId = SecurityUserContext.getCurrentUserId();
        List<String> paths = incomeSubmissionService.uploadScreenshots(userId, screenshots);

        IncomeSubmissionUploadRequest request = new IncomeSubmissionUploadRequest();
        request.setPeriodMonth(periodMonth);
        request.setAmount(amount);
        request.setPlatform(platform);
        request.setScreenshotPaths(paths);

        return Result.success(incomeSubmissionService.submit(userId, request));
    }

    @GetMapping("/income-submissions/me")
    public Result<List<IncomeSubmissionVO>> mySubmissions() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(incomeSubmissionService.listByUser(userId, null));
    }
}
