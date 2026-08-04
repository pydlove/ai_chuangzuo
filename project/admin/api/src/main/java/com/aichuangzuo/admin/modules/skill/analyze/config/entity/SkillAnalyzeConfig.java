package com.aichuangzuo.admin.modules.skill.analyze.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI 提示词分析安全配置，对应表 {@code a_skill_analyze_config}。
 *
 * <p>单行配置（id=1），由 admin 端 GET/PUT 维护。
 */
@Getter
@Setter
@TableName("a_skill_analyze_config")
public class SkillAnalyzeConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 每个用户每天最多可进行 AI 提示词分析的次数。 */
    private Integer dailyAttemptLimit;

    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
