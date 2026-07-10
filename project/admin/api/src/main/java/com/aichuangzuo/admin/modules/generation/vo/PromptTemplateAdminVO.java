package com.aichuangzuo.admin.modules.generation.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PromptTemplateAdminVO {
    private Long id;
    private String name;
    private String baseContent;
    private Integer enabled;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;

    /** 12 阶段配置（可能为空：老模板没初始化 stage）。 */
    private List<PromptTemplateStageVO> stages;

    /** 老模板是否已初始化 12 阶段（用于前端显示「初始化 12 阶段」按钮）。 */
    private Boolean stagesInitialized;

    /** 是否内置模板（id=1 的默认去 AI 味模板），内置模板禁止删除。 */
    private Boolean isBuiltin;

    /** 模板状态码：0-草稿，1-已发布，2-已下线。 */
    private Integer templateStatus;

    /** 模板状态标签（中文）。 */
    private String templateStatusLabel;

    /** 最新已发布版本号（草稿/未发布时为 null）。 */
    private Integer latestPublishedVersion;
}
