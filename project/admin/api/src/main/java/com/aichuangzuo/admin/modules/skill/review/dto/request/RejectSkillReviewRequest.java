package com.aichuangzuo.admin.modules.skill.review.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 打回风格请求体。
 */
@Data
public class RejectSkillReviewRequest {

    @NotBlank(message = "打回原因不能为空")
    @Size(max = 200, message = "打回原因最多 200 字")
    private String reason;
}