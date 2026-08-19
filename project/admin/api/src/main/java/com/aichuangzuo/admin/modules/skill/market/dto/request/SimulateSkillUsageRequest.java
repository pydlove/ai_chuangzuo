package com.aichuangzuo.admin.modules.skill.market.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SimulateSkillUsageRequest {
    @NotNull(message = "请选择使用者")
    private Long userId;
}
