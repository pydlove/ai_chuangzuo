package com.aichuangzuo.admin.modules.skill.market.config.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.skill.market.config.dto.request.SkillPricePerUseUpdateRequest;
import com.aichuangzuo.admin.modules.skill.market.config.service.SkillMonthlyRewardConfigService;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Admin 端 - 提示词市场单次使用收益单价配置 API。
 */
@Tag(name = "管理端-提示词市场单价配置")
@RestController
@RequestMapping("/api/v1/admin/skill-market/price-per-use")
@RequiredArgsConstructor
public class SkillPricePerUseController {

    private final SkillMonthlyRewardConfigService service;

    @GetMapping
    public Result<BigDecimal> get() {
        return Result.success(service.getPricePerUse());
    }

    @PutMapping
    public Result<BigDecimal> update(@Valid @RequestBody SkillPricePerUseUpdateRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        return Result.success(service.updatePricePerUse(request.getPricePerUse(), adminUserId));
    }
}
