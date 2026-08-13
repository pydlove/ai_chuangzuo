package com.aichuangzuo.admin.modules.scheduler.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 定时任务执行日志 VO。
 */
@Getter
@Setter
public class ScheduledTaskLogVO {

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
