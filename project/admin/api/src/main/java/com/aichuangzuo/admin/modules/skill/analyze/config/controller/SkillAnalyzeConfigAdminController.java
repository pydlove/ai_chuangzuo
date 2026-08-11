package com.aichuangzuo.admin.modules.skill.analyze.config.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.skill.analyze.config.dto.request.SkillAnalyzeConfigUpdateRequest;
import com.aichuangzuo.admin.modules.skill.analyze.config.service.SkillAnalyzeConfigService;
import com.aichuangzuo.admin.modules.skill.analyze.config.vo.SkillAnalyzeConfigVO;
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
 * Admin 端 - AI 提示词分析安全配置 API。
 */
@Tag(name = "管理端-AI 提示词分析安全配置")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/security/skill-analyze-config")
@RequiredArgsConstructor
public class SkillAnalyzeConfigAdminController {

    private final SkillAnalyzeConfigService service;

    @GetMapping
    public Result<SkillAnalyzeConfigVO> detail() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询 AI 提示词分析安全配置, adminUserId={}", adminUserId);
        return Result.success(service.detail());
    }

    @PutMapping
    public Result<SkillAnalyzeConfigVO> update(@Valid @RequestBody SkillAnalyzeConfigUpdateRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员更新 AI 提示词分析安全配置, adminUserId={}, dailyAttemptLimit={}",
                adminUserId, request.getDailyAttemptLimit());
        return Result.success(service.update(request, adminUserId));
    }
}
