package com.aichuangzuo.user.modules.lottery.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.lottery.dto.request.LotteryRedeemRequest;
import com.aichuangzuo.user.modules.lottery.service.LotteryRedemptionService;
import com.aichuangzuo.user.modules.lottery.vo.LotteryRedemptionResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户端-兑换码兑换")
@Slf4j
@RestController
@RequestMapping("/api/v1/user/lottery")
@RequiredArgsConstructor
public class LotteryRedemptionController {

    private final LotteryRedemptionService lotteryRedemptionService;

    @Operation(summary = "兑换奖励")
    @PostMapping("/redeem")
    public Result<LotteryRedemptionResultVO> redeem(@RequestBody @Valid LotteryRedeemRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("用户兑换奖励 userId={}, code={}", userId, request.getCode());
        return Result.success(lotteryRedemptionService.redeem(userId, request.getCode()));
    }
}
