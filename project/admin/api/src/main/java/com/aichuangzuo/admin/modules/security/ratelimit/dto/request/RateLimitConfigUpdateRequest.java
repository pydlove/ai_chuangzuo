package com.aichuangzuo.admin.modules.security.ratelimit.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class RateLimitConfigUpdateRequest {

    /** 登录限流开关：0-否，1-是。为空时不更新。 */
    private Integer isLoginRateLimitEnabled;

    /** 平台账号检测每日次数上限。为空时不更新。 */
    @Min(value = 1, message = "账号检测日限次至少为 1")
    @Max(value = 1000, message = "账号检测日限次最大为 1000")
    private Integer nicknameCheckDailyLimit;
}
