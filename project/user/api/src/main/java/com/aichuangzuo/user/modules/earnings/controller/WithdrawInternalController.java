package com.aichuangzuo.user.modules.earnings.controller;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.UnauthorizedException;
import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.AdminJwtUtil;
import com.aichuangzuo.user.modules.earnings.dto.request.WithdrawProcessRequest;
import com.aichuangzuo.user.modules.earnings.service.WithdrawService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端内部接口：供管理端处理提现申请。
 */
@RestController
@RequestMapping("/api/v1/user/internal/withdrawals")
@RequiredArgsConstructor
public class WithdrawInternalController {

    private final WithdrawService withdrawService;
    private final AdminJwtUtil adminJwtUtil;

    @PostMapping("/{bizNo}/process")
    public Result<Void> process(@RequestHeader("Authorization") String authHeader,
                                @PathVariable("bizNo") String bizNo,
                                @Valid @RequestBody WithdrawProcessRequest request) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException(UserAuthErrorCode.TOKEN_EXPIRED);
        }
        Long adminUserId = adminJwtUtil.parseAccessToken(authHeader.substring(7));
        withdrawService.processWithdraw(bizNo, adminUserId, request);
        return Result.success();
    }
}
