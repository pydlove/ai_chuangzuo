package com.aichuangzuo.admin.modules.style.review.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风格审核列表项。
 *
 * <p>字段语义已对齐管理端前端契约：
 * <ul>
 *   <li>{@code id} = {@code bizNo}（前端 row-key 用）</li>
 *   <li>{@code sourceType} 取值 {@code "my"} / {@code "learned"}</li>
 *   <li>{@code status} 取值 {@code "pending"} / {@code "approved"} / {@code "rejected"}</li>
 * </ul>
 *
 * <p>注意：mapper XML 直接用同名字段填充；{@code sourceType} / {@code status} 字段类型在 VO 里是
 * {@code String}（前端契约），Service 层在拼装时把 int 转成字符串再 set 进来。
 */
@Data
public class StyleReviewVO {

    private String id;

    private String name;

    private String sourceType;

    private String creatorName;

    private String prompt;

    private String scope;

    private String status;

    private String rejectReason;

    private LocalDateTime createdAt;
}