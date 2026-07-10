package com.aichuangzuo.admin.modules.generation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 克隆创作模板请求。
 *
 * <p>复制源模板的 12 阶段配置到一张新草稿。源模板状态不限（草稿 / 已发布 / 已下线均可）。
 *
 * <p>设计文档：§5.14.3
 */
@Data
public class CloneTemplateRequest {

    @NotBlank
    @Size(max = 64)
    private String name;

    /** 备注。 */
    @Size(max = 256)
    private String remark;

    /**
     * 可选：复制源模板的某个历史版本（从 {@code t_prompt_template_version} 取）。
     * 不传则取源模板当前的 stage 表（草稿或最新已发布阶段的实际生效配置）。
     */
    private Integer sourceVersion;
}