package com.aichuangzuo.user.modules.security.ratelimit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 登录限流配置，对应表 {@code a_rate_limit_config}。
 *
 * <p>单行配置（id=1），由 admin 端维护，用户端读取。
 */
@Getter
@Setter
@TableName("a_rate_limit_config")
public class RateLimitConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 是否启用登录限流：0-否，1-是。 */
    private Integer isLoginRateLimitEnabled;

    /** 平台账号检测每日次数上限，默认 10。 */
    private Integer nicknameCheckDailyLimit;

    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
