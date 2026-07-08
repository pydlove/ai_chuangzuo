package com.aichuangzuo.admin.modules.reminder.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ReminderConfigRequest {
    @NotNull(message = "提前天数不能为空")
    private Integer advanceDays;

    @NotNull(message = "提醒时间点不能为空")
    private Integer notifyHour;

    @NotNull(message = "通知形式不能为空")
    @Pattern(regexp = "message|email|message_email", message = "通知形式非法")
    private String notifyChannel;

    @NotNull(message = "开关不能为空")
    private Integer enabled;
}