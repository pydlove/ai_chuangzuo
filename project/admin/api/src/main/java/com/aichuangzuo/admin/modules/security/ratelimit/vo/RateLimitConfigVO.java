package com.aichuangzuo.admin.modules.security.ratelimit.vo;

import lombok.Data;

@Data
public class RateLimitConfigVO {

    private Long id;

    /** 是否启用登录限流：0-否，1-是。 */
    private Integer isLoginRateLimitEnabled;

    /** 平台账号检测每日次数上限，默认 10。 */
    private Integer nicknameCheckDailyLimit;
}
