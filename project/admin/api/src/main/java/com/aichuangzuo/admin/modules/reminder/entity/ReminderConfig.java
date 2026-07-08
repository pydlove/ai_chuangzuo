package com.aichuangzuo.admin.modules.reminder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 到期提醒配置，对应表 reminder_config。
 * 单行表，固定 id=1。
 */
@Getter
@Setter
@TableName("reminder_config")
public class ReminderConfig {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 提前提醒天数 N（命中区间：0 ≤ remainingDays ≤ N）。 */
    private Integer advanceDays;

    /** 每天提醒时间点 0-23。 */
    private Integer notifyHour;

    /** message / email / message_email。 */
    private String notifyChannel;

    /** 0-关 1-开。 */
    private Integer enabled;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}