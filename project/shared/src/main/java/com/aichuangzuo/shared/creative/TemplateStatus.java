package com.aichuangzuo.shared.creative;

import java.util.Arrays;

/**
 * 创作模板状态机。
 *
 * <p>设计文档：docs/superpowers/specs/2026-07-09-de-ai-flavor-writing-pipeline-design.md §5.14.2
 *
 * <p>流转：{@link #DRAFT} → {@link #PUBLISHED} → {@link #OFFLINE} → {@link #PUBLISHED}（重新发布）
 */
public enum TemplateStatus {

    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    OFFLINE(2, "已下线");

    public final int code;
    public final String label;

    TemplateStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static TemplateStatus fromCode(Integer code) {
        if (code == null) return DRAFT;
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElse(DRAFT);
    }
}