package com.aichuangzuo.admin.modules.generation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 12 阶段配置中单个 stage 的更新项。
 *
 * <p>按 stageIndex 定位；aiPrompt / ruleConfig 哪个非空取决于 stageType。
 */
@Data
public class PromptTemplateStageSaveItem {

    @NotNull
    private Integer stageIndex;

    /** 0=禁用，1=启用。 */
    private Integer enabled;

    /** stage_type=ai_prompt 时填。 */
    private String aiPrompt;

    /** stage_type=rule_config 时填（JSON 字符串）。 */
    private String ruleConfig;

    /** 可选：AI 阶段可配参数（JSON 对象，如 {"temperature":0.7,"max_tokens":2000}）。 */
    private Map<String, Object> modelParams;
}
