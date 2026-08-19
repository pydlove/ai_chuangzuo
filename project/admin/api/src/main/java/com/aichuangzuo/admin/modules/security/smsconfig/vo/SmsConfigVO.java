
package com.aichuangzuo.admin.modules.security.smsconfig.vo;

import lombok.Data;

@Data
public class SmsConfigVO {
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
}
