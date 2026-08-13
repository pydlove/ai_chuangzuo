package com.aichuangzuo.admin.modules.leaderboard.config.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.leaderboard.config.dto.request.LeaderboardRewardConfigUpdateRequest;
import com.aichuangzuo.admin.modules.leaderboard.config.service.LeaderboardRewardConfigService;
import com.aichuangzuo.admin.modules.leaderboard.config.vo.LeaderboardRewardConfigVO;
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
 * Admin 端 - 收益排行榜奖励规则配置 API。
 */
@Tag(name = "管理端-收益排行榜奖励配置")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/leaderboards/reward-config")
@RequiredArgsConstructor
public class LeaderboardRewardConfigAdminController {

    private final LeaderboardRewardConfigService service;

    @GetMapping
    public Result<LeaderboardRewardConfigVO> detail() {
        Long adminUserId = currentAdminId();
        log.info("管理员查询收益排行榜奖励配置, adminUserId={}", adminUserId);
        return Result.success(service.detail());
    }

    @PutMapping
    public Result<LeaderboardRewardConfigVO> update(@Valid @RequestBody LeaderboardRewardConfigUpdateRequest request) {
        Long adminUserId = currentAdminId();
        log.info("管理员更新收益排行榜奖励配置, adminUserId={}, topLimit={}, rewardAmount={}",
                adminUserId, request.getTopLimit(), request.getRewardAmount());
        return Result.success(service.update(request, adminUserId));
    }

    private Long currentAdminId() {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        return adminId != null ? adminId : 0L;
    }
}
