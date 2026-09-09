
package com.aichuangzuo.admin.modules.security.smsconfig.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SmsSendRecordQueryRequest {

    /** 手机号模糊查询。 */
    private String phone;

    /** 发送场景：register / reset_password / bind_phone。 */
    private String scene;

    /** 发送状态：0-失败，1-成功。 */
    private Integer sendStatus;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Long page = 1L;
    private Long size = 20L;
}
