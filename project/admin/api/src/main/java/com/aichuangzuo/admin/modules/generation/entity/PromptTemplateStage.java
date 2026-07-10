package com.aichuangzuo.admin.modules.generation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 创作提示词模板 12 阶段配置，对应表 {@code t_prompt_template_stage}。
 *
 * <p>每行代表模板下的一个阶段（1-12），按 stage_type 决定填充 ai_prompt 或 rule_config。
 * 详见设计文档 2026-07-09-de-ai-flavor-writing-pipeline-design.md。
 */
@Getter
@Setter
@TableName("t_prompt_template_stage")
public class PromptTemplateStage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属模板 ID（t_prompt_template.id）。 */
    private Long templateId;

    /** 阶段序号 1-12。 */
    private Integer stageIndex;

    /** 阶段类型：ai_prompt / rule_config / passthrough。 */
    private String stageType;

    /** 阶段稳定标识符：outline / material_list / draft / ... */
    private String stageKey;

    /** 仅 stage_type=ai_prompt 有值。 */
    private String aiPrompt;

    /** 仅 stage_type=rule_config 有值（JSON 字符串）。 */
    private String ruleConfig;

    /** 该 stage 是否启用。 */
    private Integer enabled;

    private Long tenantId;
    @TableLogic
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
