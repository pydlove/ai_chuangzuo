package com.aichuangzuo.admin.modules.earnings.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LeaderboardRejectRequest {

    @NotBlank(message = "拒绝原因不能为空")
    private String rejectReason;
}
