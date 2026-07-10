package com.aichuangzuo.admin.modules.generation.pipeline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 规则配置阶段的单个表单字段描述（前端用来渲染 form）。
 *
 * <p>数据从 {@link PipelineStage#configFields} 读，写回 stage 的 {@code ruleConfig} JSON。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigField {

    /** 字段 key（在 ruleConfig JSON 里的 key）。 */
    private String key;

    /** 字段显示名。 */
    private String label;

    /** 字段类型：number / boolean / select / text。 */
    private String type;

    /** 默认值。 */
    private Object defaultValue;

    /** 数字类型的范围。 */
    private Integer min;
    private Integer max;

    /** select 类型的可选项。 */
    private List<Option> options;

    /** 帮助说明。 */
    private String description;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private String label;
        private String value;
    }
}
