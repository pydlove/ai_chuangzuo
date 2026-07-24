package com.aichuangzuo.admin.modules.style.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 风格审核列表查询参数。
 *
 * <p>{@code status} 为 null 表示查询全部状态。
 */
@Data
public class StyleReviewPageRequest {

    /** 审核状态过滤：0-待审核，1-已通过，2-已拒绝，null-全部。 */
    private Integer status;

    /** 是否只查询已审核记录（已通过 + 已打回），优先级高于 {@code status}。 */
    private Boolean reviewed;

    /** 搜索关键词（风格名 / 创作者昵称）。 */
    private String keyword;

    @Min(value = 1, message = "pageNum 必须 ≥ 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "pageSize 必须 ≥ 1")
    @Max(value = 100, message = "pageSize 不能超过 100")
    private Integer pageSize = 20;
}