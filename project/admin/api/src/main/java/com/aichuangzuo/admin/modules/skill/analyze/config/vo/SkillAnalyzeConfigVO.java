package com.aichuangzuo.admin.modules.skill.analyze.config.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 提示词分析安全配置视图对象。
 */
@Data
public class SkillAnalyzeConfigVO {

    private Long id;
    private Integer dailyAttemptLimit;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
