package com.aichuangzuo.admin.modules.generation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新建 / 编辑提示词模板。
 *
 * <p>{@code stages} 长度必须 = 13（按 stageIndex 1-13），覆盖完整流水线。
 * 系统提示词 / 用户风格按设计文档约定由各 stage prompt 内部处理，不在这里。
 */
@Data
public class PromptTemplateSaveRequest {

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 64)
    private String name;

    /** 保留字段：当前仍由旧 executor 读取；新 UI 不再编辑它。 */
    @Size(max = 100_000)
    private String baseContent;

    @Size(max = 256)
    private String remark;

    /** 13 个 stage 配置（编辑时必须完整传 13 个；新建时为空则由后端按 PipelineStage 默认补齐）。 */
    @NotNull
    @Size(min = 0, max = 13, message = "stage 数量不能超过 13 个")
    @Valid
    private List<PromptTemplateStageSaveItem> stages;
}
