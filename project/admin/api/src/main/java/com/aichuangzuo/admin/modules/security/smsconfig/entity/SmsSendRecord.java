
package com.aichuangzuo.admin.modules.security.smsconfig.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 短信发送记录，对应表 u_sms_send_record（用户端写入，管理端只读查询）。
 */
@Getter
@Setter
@TableName("u_sms_send_record")
public class SmsSendRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    /** 发送场景：register-注册，reset_password-忘记密码，bind_phone-绑定/换绑手机号。 */
    private String scene;

    private String clientIp;

    /** 触发用户ID，未登录为 0。 */
    private Long userId;

    /** 发送状态：0-失败，1-成功。 */
    private Integer sendStatus;

    private String failReason;

    /** 服务商返回码。 */
    private String responseCode;

    private Long tenantId;

    private LocalDateTime createdAt;
}
