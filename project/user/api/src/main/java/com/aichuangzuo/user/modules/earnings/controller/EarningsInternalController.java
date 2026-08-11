package com.aichuangzuo.user.modules.earnings.controller;

import com.aichuangzuo.shared.enums.error.UserAuthErrorCode;
import com.aichuangzuo.shared.exception.UnauthorizedException;
import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.AdminJwtUtil;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.earnings.dto.request.RecordEarningsRequest;
import com.aichuangzuo.user.modules.earnings.service.EarningsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端内部接口：供管理端调用记录收益。
 */
@RestController
@RequestMapping("/api/v1/user/internal/earnings")
@RequiredArgsConstructor
@Slf4j
public class EarningsInternalController {

    private final EarningsService earningsService;
    private final AdminJwtUtil adminJwtUtil;

    @PostMapping("/record")
    public Result<Void> record(@RequestHeader("Authorization") String authHeader,
                               @Valid @RequestBody RecordEarningsRequest request) {
        Long currentUserId = SecurityUserContext.getCurrentUserId();
        log.info("Record earnings, currentUserId={}, userId={}, type={}, sourceType={}, sourceId={}, title={}, amount={}, settlementMonth={}",
                currentUserId, request.getUserId(), request.getType(), request.getSourceType(),
                request.getSourceId(), request.getTitle(), request.getAmount(), request.getSettlementMonth());
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException(UserAuthErrorCode.TOKEN_EXPIRED);
        }
        adminJwtUtil.parseAccessToken(authHeader.substring(7));

        earningsService.recordEarnings(request.getUserId(), request.getType(), request.getSourceType(),
                request.getSourceId(), request.getTitle(), request.getDescription(),
                request.getAmount(), request.getSettlementMonth());
        return Result.success();
    }
}
