package com.aichuangzuo.admin.modules.hotsearch.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HotSearchConfigRequest {
    @NotBlank
    private String cron;
    @NotNull
    private Integer enabled;
    @NotNull
    @Min(1)
    private Integer topN;
    @NotNull
    @Min(100)
    private Integer connectTimeoutMillis;
    @NotNull
    @Min(100)
    private Integer readTimeoutMillis;
}
