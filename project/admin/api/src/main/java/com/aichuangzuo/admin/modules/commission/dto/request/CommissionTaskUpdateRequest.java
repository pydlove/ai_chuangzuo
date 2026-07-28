package com.aichuangzuo.admin.modules.commission.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CommissionTaskUpdateRequest {
    @NotBlank
    @Size(max = 128)
    private String title;
    @NotBlank
    private String description;
    @NotNull
    @Min(1)
    private Integer minWordCount;
    @NotNull
    @Min(1)
    private Integer maxWordCount;
    @Size(max = 128)
    private String skillHint;
    @NotNull
    @DecimalMin("5")
    private BigDecimal rewardCoin;
    @NotNull
    @Min(1)
    private Integer neededCount;
    @NotNull
    private LocalDateTime deadlineAt;
    @NotNull
    private LocalDateTime selectionDeadlineAt;
}