package com.aichuangzuo.admin.modules.earnings.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class SettlementRequest {

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "月份格式必须为 YYYY-MM")
    private String month;

    private List<Long> userIds;
}
