package com.aichuangzuo.user.modules.generation.vo;

import lombok.Data;

/**
 * user 端公开的模板信息（不含 prompt 全文，仅展示用）。
 *
 * <p>设计文档：§5.15.4
 */
@Data
public class PromptTemplatePublicVO {
    private Long id;
    private String name;
    private String remark;
    private Integer latestPublishedVersion;
    private Boolean isBuiltin;
}