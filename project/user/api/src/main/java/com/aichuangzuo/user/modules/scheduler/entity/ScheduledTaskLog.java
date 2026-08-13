package com.aichuangzuo.user.modules.scheduler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 定时任务执行日志，对应表 u_scheduled_task_log。
 */
@Getter
@Setter
@TableName("u_scheduled_task_log")
public class ScheduledTaskLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String triggerType;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private String runStatus;

    private String message;

    private Long createdBy;

    private LocalDateTime createdAt;
}
