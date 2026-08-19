
package com.aichuangzuo.admin.modules.security.smsconfig.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 短信配置，对应表 {@code a_sms_config}。
 *
 * <p>单行配置（id=1），由 admin 端系统设置-短信配置维护。
 * AccessKeySecret 落库为 Jasypt 加密后的密文。
 */
@Getter
@Setter
@TableName("a_sms_config")
public class SmsConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 短信服务商：当前仅 aliyun。 */
    private String provider;

    /** AccessKey ID。 */
    private String accessKeyId;

    /** Jasypt 加密后的 AccessKey Secret。 */
    private String accessKeySecret;

    /** 短信签名。 */
    private String signName;

    /** 短信模板 Code。 */
    private String templateCode;

    /** 阿里云区域 ID，默认 cn-hangzhou。 */
    private String regionId;

    /** 是否启用短信：0-否，1-是。 */
    private Integer enabled;

    /** 同一手机号两次发送最小间隔（秒）。 */
    private Integer sendIntervalSeconds;

    /** 同一手机号每天最多发送次数。 */
    private Integer dailyMaxPerPhone;

    /** 同一 IP 每天最多发送次数。 */
    private Integer dailyMaxPerIp;

    /** 全站每天最多发送次数。 */
    private Integer globalDailyMax;

    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
