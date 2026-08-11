package com.aichuangzuo.admin.modules.security.ratelimit.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RateLimitConfigUpdateRequest {

    @NotNull(message = "登录限流开关不能为空")
    private Integer isLoginRateLimitEnabled;
}
