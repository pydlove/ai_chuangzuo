package com.aichuangzuo.user.modules.leaderboard.controller;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.UnauthorizedException;
import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.AdminJwtUtil;
import com.aichuangzuo.user.modules.leaderboard.dto.request.CoinRecordGrantRequest;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端内部接口：供管理端调用发放创作币奖励。
 */
@RestController
@RequestMapping("/api/v1/user/internal/coin-records")
@RequiredArgsConstructor
public class CoinRecordInternalController {

    private final CoinRecordService coinRecordService;
    private final AdminJwtUtil adminJwtUtil;

    @PostMapping("/grant")
    public Result<String> grant(@RequestHeader("Authorization") String authHeader,
                                  @Valid @RequestBody CoinRecordGrantRequest request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException(UserAuthErrorCode.TOKEN_EXPIRED);
        }
        adminJwtUtil.parseAccessToken(authHeader.substring(7));

        String bizNo = coinRecordService.grant(request.getUserId(), "leaderboard_reward",
                request.getAmount(), request.getRefId(), request.getRemark());
        return Result.success(bizNo);
    }
}
