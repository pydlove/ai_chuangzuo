
package com.aichuangzuo.admin.modules.security.smsconfig.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SmsConfigUpdateRequest {

    @NotBlank(message = "服务商不能为空")
    private String provider;

    @NotBlank(message = "AccessKey ID 不能为空")
    private String accessKeyId;

    /** 为空或全 * 号表示不修改原密钥。 */
    private String accessKeySecret;

    @NotBlank(message = "短信签名不能为空")
    private String signName;

    @NotBlank(message = "短信模板 Code 不能为空")
    private String templateCode;

    @NotBlank(message = "区域 ID 不能为空")
    private String regionId;

    @NotNull(message = "启用状态不能为空")
    private Integer enabled;

    @NotNull(message = "发送间隔不能为空")
    @Min(value = 1, message = "发送间隔至少 1 秒")
    @Max(value = 3600, message = "发送间隔最大 3600 秒")
    private Integer sendIntervalSeconds;

    @NotNull(message = "单手机号日限不能为空")
    @Min(value = 1, message = "单手机号日限至少 1 条")
    @Max(value = 100, message = "单手机号日限最大 100 条")
    private Integer dailyMaxPerPhone;

    @NotNull(message = "单 IP 日限不能为空")
    @Min(value = 1, message = "单 IP 日限至少 1 条")
    @Max(value = 1000, message = "单 IP 日限最大 1000 条")
    private Integer dailyMaxPerIp;

    @NotNull(message = "全站日限不能为空")
    @Min(value = 1, message = "全站日限至少 1 条")
    @Max(value = 100000, message = "全站日限最大 100000 条")
    private Integer globalDailyMax;
}
