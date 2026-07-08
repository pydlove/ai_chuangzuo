package com.aichuangzuo.admin.modules.style.market.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 风格市场列表查询参数。
 *
 * <p>{@code enableStatus} 为 null 表示查询全部。
 */
@Data
public class StyleMarketPageRequest {

    /** 启用状态过滤：0-禁用，1-启用，null-全部。 */
    private Integer enableStatus;

    /** 搜索关键词（按风格名或发布者昵称模糊匹配）。 */
    private String keyword;

    @Min(value = 1, message = "pageNum 必须 ≥ 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "pageSize 必须 ≥ 1")
    @Max(value = 100, message = "pageSize 不能超过 100")
    private Integer pageSize = 20;
}
