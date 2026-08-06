package com.aichuangzuo.user.modules.skill.market.controller;

import com.aichuangzuo.shared.enums.error.UserGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.skill.market.service.SkillMarketUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户端内部接口：供管理端（生成 pipeline / 审核 worker）调用，记录市场提示词使用及释放发布额度。
 * <p>由 {@code InternalKeyAuthenticationFilter} 校验 {@code X-Internal-Key}。
 */
@RestController
@RequestMapping("/api/v1/user/internal/market-skills")
@RequiredArgsConstructor
public class SkillMarketInternalController {

    private static final String BENEFIT_CODE_SKILL_MARKET_PUBLISH = "skill_market_publish";

    private final SkillMarketUsageService usageService;
    private final BenefitService benefitService;

    @PostMapping("/{bizNo}/use")
    public Result<Void> recordUsage(@PathVariable String bizNo,
                                       @RequestParam Long consumerUserId) {
        usageService.recordUsage(bizNo, consumerUserId);
        return Result.success();
    }

    /**
     * 管理端审核打回时调用：退回用户发布到提示词市场时消耗的额度。
     *
     * <p>payload 字段：
     * <ul>
     *   <li>userId 必填</li>
     * </ul>
     */
    @PostMapping("/refund-publish-quota")
    public Result<Void> refundPublishQuota(@RequestBody Map<String, Object> payload) {
        Long userId = asLong(payload.get("userId"));
        if (userId == null) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_INPUT_INVALID);
        }
        benefitService.refund(userId, BENEFIT_CODE_SKILL_MARKET_PUBLISH);
        return Result.success();
    }

    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }
}
