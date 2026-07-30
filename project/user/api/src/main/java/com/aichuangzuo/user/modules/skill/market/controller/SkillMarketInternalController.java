package com.aichuangzuo.user.modules.skill.market.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.skill.market.service.SkillMarketUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端内部接口：供管理端（生成 pipeline）调用，记录市场提示词使用。
 * <p>由 {@code InternalKeyAuthenticationFilter} 校验 {@code X-Internal-Key}。
 */
@RestController
@RequestMapping("/api/v1/user/internal/market-skills")
@RequiredArgsConstructor
public class SkillMarketInternalController {

    private final SkillMarketUsageService usageService;

    @PostMapping("/{bizNo}/use")
    public Result<Void> recordUsage(@PathVariable String bizNo,
                                       @RequestParam Long consumerUserId) {
        usageService.recordUsage(bizNo, consumerUserId);
        return Result.success();
    }
}
