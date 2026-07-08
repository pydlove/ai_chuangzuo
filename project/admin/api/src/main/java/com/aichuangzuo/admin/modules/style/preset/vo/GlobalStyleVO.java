package com.aichuangzuo.admin.modules.style.preset.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预设风格视图对象（前端契约）。
 *
 * <p>{@code id} = {@code bizNo}（前端 row-key），{@code status} 取值 {@code "enabled"} / {@code "disabled"}。
 */
@Data
public class GlobalStyleVO {

    private String id;
    private String name;
    private String description;
    private String promptSummary;
    private String prompt;
    private String scope;
    private String status;
    private String creatorName;
    private LocalDateTime createdAt;
}