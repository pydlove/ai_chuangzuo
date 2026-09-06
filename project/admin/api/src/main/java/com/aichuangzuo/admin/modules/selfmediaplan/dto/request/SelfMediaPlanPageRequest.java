package com.aichuangzuo.admin.modules.selfmediaplan.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 用户运营方案列表查询参数。
 */
@Data
public class SelfMediaPlanPageRequest {

    /** 搜索关键词（按用户昵称、手机号或邮箱模糊匹配）。 */
    private String keyword;

    @Min(value = 1, message = "page 必须 ≥ 1")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize 必须 ≥ 1")
    @Max(value = 100, message = "pageSize 不能超过 100")
    private Integer pageSize = 10;
}
