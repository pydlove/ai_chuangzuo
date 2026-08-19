
package com.aichuangzuo.user.modules.security.smsconfig.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 短信配置，对应管理端表 {@code a_sms_config}。
 *
 * <p>单行配置（id=1），用户端仅读取，admin 端维护。
 */
@Getter
@Setter
@TableName("a_sms_config")
public class SmsConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String provider;
    private String accessKeyId;
    private String accessKeySecret;
    private String signName;
    private String templateCode;
    private String regionId;
    private Integer enabled;
    private Integer sendIntervalSeconds;
    private Integer dailyMaxPerPhone;
    private Integer dailyMaxPerIp;
    private Integer globalDailyMax;

    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
