package com.aichuangzuo.user.modules.earnings.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 查询用户收益记录请求。
 */
@Data
public class ListEarningsRequest {

    /**
     * 结算状态：all / settled / unsettled。
     */
    private String status = "all";

    /**
     * 归属月份，格式 YYYY-MM。
     */
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式应为 YYYY-MM")
    private String month;

    @Min(value = 1, message = "页码至少为 1")
    private Integer page = 1;

    @Min(value = 1, message = "每页至少 1 条")
    @Max(value = 100, message = "每页最多 100 条")
    private Integer pageSize = 20;
}
