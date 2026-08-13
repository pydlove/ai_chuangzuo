package com.aichuangzuo.admin.modules.scheduler.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 定时任务列表 VO。
 */
@Getter
@Setter
public class ScheduledTaskVO {

    private Long id;

    private String taskKey;

    private String taskName;

    private String description;

    private String module;

    private String triggerType;

    private String expression;

    private Integer enabled;

    private LocalDateTime lastRunAt;

    private String lastRunStatus;

    private String lastRunMessage;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
