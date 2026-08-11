package com.aichuangzuo.admin.modules.security.ratelimit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 登录限流配置，对应表 {@code a_rate_limit_config}。
 *
 * <p>单行配置（id=1），由 admin 端 GET/PUT 维护。
 */
@Getter
@Setter
@TableName("a_rate_limit_config")
public class RateLimitConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 是否启用登录限流：0-否，1-是。 */
    private Integer isLoginRateLimitEnabled;

    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
