package com.aichuangzuo.admin.modules.generation.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创作模板版本摘要（不含 config_json 全文，详情接口单独返回）。
 */
@Data
public class PromptTemplateVersionVO {
    private Integer version;
    private Integer versionStatus;
    private String versionStatusLabel;
    private String changeNote;
    private LocalDateTime publishedAt;
    private Long publishedBy;
}