package com.aichuangzuo.admin.modules.feedback.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminReplyFeedbackRequest {
    @NotBlank(message = "回复内容不能为空")
    @Size(max = 2000, message = "回复最多 2000 字")
    private String content;
}
