package com.aichuangzuo.admin.modules.reminder.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 到期提醒配置默认值，启动时若 DB 不存在则同步进来。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "reminder")
public class ReminderProperties {

    /** 提前提醒天数 N。 */
    private Integer advanceDays = 7;

    /** 每天提醒时间点 0-23。 */
    private Integer notifyHour = 9;

    /** message / email / message_email。 */
    private String notifyChannel = "message";

    /** 定时提醒开关。 */
    private boolean enabled = true;
}