package com.aichuangzuo.admin.modules.order.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class RenewalUserQueryRequest {
    private String keyword;
    private String planKey;
    private String cycle;
    private String startDate;
    private String endDate;

    private Boolean renewalOnly = true;

    @Min(1)
    private int page = 1;

    @Min(1)
    private int pageSize = 20;
}
