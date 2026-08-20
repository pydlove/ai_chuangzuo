package com.aichuangzuo.admin.modules.security.ratelimit.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.security.ratelimit.dto.request.RateLimitConfigUpdateRequest;
import com.aichuangzuo.admin.modules.security.ratelimit.service.RateLimitConfigService;
import com.aichuangzuo.admin.modules.security.ratelimit.vo.RateLimitConfigVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin 端 - 登录限流安全配置 API。
 */
@Tag(name = "管理端-登录限流安全配置")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/security/rate-limit-config")
@RequiredArgsConstructor
public class RateLimitConfigAdminController {

    private final RateLimitConfigService service;

    @GetMapping
    public Result<RateLimitConfigVO> detail() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询登录限流安全配置, adminUserId={}", adminUserId);
        return Result.success(service.detail());
    }

    @PutMapping
    public Result<RateLimitConfigVO> update(@Valid @RequestBody RateLimitConfigUpdateRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员更新登录限流安全配置, adminUserId={}, isLoginRateLimitEnabled={}, nicknameCheckDailyLimit={}",
                adminUserId, request.getIsLoginRateLimitEnabled(), request.getNicknameCheckDailyLimit());
        return Result.success(service.update(request, adminUserId));
    }
}
