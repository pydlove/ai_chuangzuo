package com.aichuangzuo.admin.modules.hotsearch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class HotSearchDailyRequest {
    @NotBlank
    private String platformCode;
    @NotNull
    private Integer rankNum;
    @NotBlank
    @Size(max = 512)
    private String title;
    @Size(max = 64)
    private String hotValue;
    @Size(max = 1024)
    private String url;
    private Long searchCount;
    @NotNull
    private LocalDate snapshotDate;
}
