package com.aichuangzuo.admin.modules.commission.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CommissionSubmissionBatchCreateRequest {

    @NotEmpty(message = "请至少选择一位投稿用户")
    private List<@NotNull(message = "投稿用户ID不能为空") @Min(value = 1, message = "投稿用户ID必须大于0") Long> submitterIds;

    @Size(max = 256)
    private String articleTitle;

    @Size(max = 20000)
    private String articleBody;

    @Min(1)
    private Integer wordCount;
}
