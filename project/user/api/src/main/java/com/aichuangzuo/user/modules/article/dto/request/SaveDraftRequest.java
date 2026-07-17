package com.aichuangzuo.user.modules.article.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 保存草稿请求。
 */
@Data
public class SaveDraftRequest {

    @Size(max = 256)
    private String customTitle;

    private String customRequirement;

    @Size(max = 32)
    private String platform;

    private Integer wordCount;

    @Size(max = 64)
    private String style;

    @Size(max = 64)
    private String template;

    /**
     * 创建模式：guided-引导模式，minimal-熟手模式。
     */
    @Size(max = 16)
    private String createMode;
}