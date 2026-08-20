package com.aichuangzuo.admin.modules.security.ratelimit.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RateLimitConfigUpdateRequest {

    @NotNull(message = "登录限流开关不能为空")
    private Integer isLoginRateLimitEnabled;

    @NotNull(message = "账号检测日限次不能为空")
    @Min(value = 1, message = "账号检测日限次至少为 1")
    @Max(value = 1000, message = "账号检测日限次最大为 1000")
    private Integer nicknameCheckDailyLimit;
}
