package com.aichuangzuo.admin.modules.earnings.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LeaderboardGrantRequest {

    @Min(1)
    @Max(2)
    private Integer leaderboardType;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}")
    private String periodMonth;
}
