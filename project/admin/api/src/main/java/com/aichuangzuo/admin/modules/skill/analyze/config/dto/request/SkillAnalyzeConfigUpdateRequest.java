package com.aichuangzuo.admin.modules.skill.analyze.config.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 提示词分析安全配置更新请求。
 */
@Data
public class SkillAnalyzeConfigUpdateRequest {

    @NotNull(message = "每日分析次数上限不能为空")
    @Min(value = 1, message = "每日分析次数上限至少为 1")
    @Max(value = 1000, message = "每日分析次数上限不能超过 1000")
    private Integer dailyAttemptLimit;
}
