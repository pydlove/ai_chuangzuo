package com.aichuangzuo.admin.modules.reminder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 到期提醒发送记录，对应表 u_reminder_send_log。
 * 唯一键 (user_id, channel, send_date) 负责去重。
 */
@Getter
@Setter
@TableName("u_reminder_send_log")
public class ReminderSendLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String channel;
    private LocalDate sendDate;
    private Integer remainingDays;
    private String triggerType;
    private Integer status;
    private String failReason;
    private LocalDateTime createdAt;
}