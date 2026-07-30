package com.aichuangzuo.admin.modules.skill.market.config.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.skill.market.config.dto.request.SkillMonthlyRewardConfigUpdateRequest;
import com.aichuangzuo.admin.modules.skill.market.config.service.SkillMonthlyRewardConfigService;
import com.aichuangzuo.admin.modules.skill.market.config.vo.SkillMonthlyRewardConfigVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin 端 - 提示词市场月度排行榜奖励配置 API。
 */
@Tag(name = "管理端-提示词市场月度奖励配置")
@RestController
@RequestMapping("/api/v1/admin/skill-market/monthly-reward-config")
@RequiredArgsConstructor
public class SkillMonthlyRewardConfigController {

    private final SkillMonthlyRewardConfigService service;

    @GetMapping
    public Result<SkillMonthlyRewardConfigVO> detail() {
        return Result.success(service.detail());
    }

    @PutMapping
    public Result<SkillMonthlyRewardConfigVO> update(@Valid @RequestBody SkillMonthlyRewardConfigUpdateRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        return Result.success(service.update(request, adminUserId));
    }
}
