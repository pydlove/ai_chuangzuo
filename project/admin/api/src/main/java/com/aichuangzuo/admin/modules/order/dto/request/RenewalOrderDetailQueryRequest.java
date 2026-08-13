package com.aichuangzuo.admin.modules.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RenewalOrderDetailQueryRequest {

    @NotBlank
    private String type;

    private String keyword;
    private String planKey;
    private String cycle;
    private String startDate;
    private String endDate;

    @Min(1)
    private int page = 1;

    @Min(1)
    private int pageSize = 20;
}
