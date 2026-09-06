package com.aichuangzuo.user.modules.lottery.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.lottery.service.LotteryDisplayService;
import com.aichuangzuo.user.modules.lottery.vo.LotteryDisplayWinnerPageVO;
import com.aichuangzuo.user.modules.lottery.vo.LotteryDisplayWinnerVO;
import com.aichuangzuo.user.modules.lottery.vo.LotteryRedemptionCodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户端-抽奖展示")
@RestController
@RequestMapping("/api/v1/user/lottery")
@RequiredArgsConstructor
public class LotteryDisplayController {

    private final LotteryDisplayService lotteryDisplayService;

    @Operation(summary = "中奖展示墙分页")
    @GetMapping("/display-winners")
    public Result<LotteryDisplayWinnerPageVO> displayWinnersPaged(
            @RequestParam Long campaignId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        int cappedPageSize = Math.min(Math.max(pageSize, 1), 50);
        return Result.success(lotteryDisplayService.listDisplayWinnersPage(campaignId, Math.max(page, 1), cappedPageSize));
    }

    @Operation(summary = "大奖得主（特等/一等/二等）")
    @GetMapping("/display-winners/grand")
    public Result<List<LotteryDisplayWinnerVO>> grandWinners(@RequestParam Long campaignId) {
        return Result.success(lotteryDisplayService.listGrandWinners(campaignId));
    }

    @Operation(summary = "我的兑换码")
    @GetMapping("/my-codes")
    public Result<List<LotteryRedemptionCodeVO>> myCodes() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(lotteryDisplayService.listMyRedemptionCodes(userId));
    }
}
