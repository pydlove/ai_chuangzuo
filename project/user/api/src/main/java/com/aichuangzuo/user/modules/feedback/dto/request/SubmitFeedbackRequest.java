package com.aichuangzuo.user.modules.feedback.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitFeedbackRequest {
    @NotBlank(message = "反馈类型不能为空")
    private String type;

    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 2000, message = "反馈内容最多 2000 字")
    private String content;
}
