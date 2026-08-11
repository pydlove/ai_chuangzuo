package com.aichuangzuo.user.modules.lottery.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.lottery.service.LotteryDisplayService;
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

    @Operation(summary = "中奖展示墙")
    @GetMapping("/display-winners")
    public Result<List<LotteryDisplayWinnerVO>> displayWinners(
            @RequestParam Long campaignId,
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(lotteryDisplayService.listDisplayWinners(campaignId, Math.min(limit, 100)));
    }

    @Operation(summary = "我的兑换码")
    @GetMapping("/my-codes")
    public Result<List<LotteryRedemptionCodeVO>> myCodes() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(lotteryDisplayService.listMyRedemptionCodes(userId));
    }
}
