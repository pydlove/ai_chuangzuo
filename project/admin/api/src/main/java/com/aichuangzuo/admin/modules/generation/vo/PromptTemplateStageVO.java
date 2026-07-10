package com.aichuangzuo.admin.modules.generation.vo;

import lombok.Data;

import java.util.List;

/**
 * 12 阶段配置详情（前端用）。
 */
@Data
public class PromptTemplateStageVO {
    private Integer stageIndex;
    private String stageType;
    private String stageKey;
    private String displayName;
    private String typeLabel;
    private String description;
    private Integer enabled;

    private String aiPrompt;
    private String ruleConfig;

    /** AI 阶段或可消费阶段可用占位符（前端 chip 渲染）。 */
    private List<StagePlaceholderVO> placeholders;

    /** 仅 rule_config 阶段有：表单字段定义。 */
    private List<StageConfigFieldVO> configFields;

    @Data
    public static class StagePlaceholderVO {
        private String name;
        private String desc;
    }

    @Data
    public static class StageConfigFieldVO {
        private String key;
        private String label;
        private String type;
        private Object defaultValue;
        private Integer min;
        private Integer max;
        private List<Option> options;
        private String description;

        @Data
        public static class Option {
            private String label;
            private String value;
        }
    }
}
