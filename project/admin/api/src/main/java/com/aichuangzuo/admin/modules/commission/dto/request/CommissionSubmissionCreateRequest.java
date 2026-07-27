package com.aichuangzuo.admin.modules.commission.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CommissionSubmissionCreateRequest {

    @NotNull(message = "请选择投稿用户")
    @Min(1)
    private Long submitterId;

    @Size(max = 256)
    private String articleTitle;

    @Size(max = 20000)
    private String articleBody;

    @Min(1)
    private Integer wordCount;
}
