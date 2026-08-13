package com.aichuangzuo.admin.modules.scheduler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 定时任务元数据，对应表 a_scheduled_task。
 */
@Getter
@Setter
@TableName("a_scheduled_task")
public class ScheduledTaskEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskKey;

    private String taskName;

    private String description;

    private String module;

    private String triggerType;

    private String expression;

    private String beanName;

    private String methodName;

    private Integer enabled;

    private LocalDateTime lastRunAt;

    private String lastRunStatus;

    private String lastRunMessage;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long createdBy;

    private Long updatedBy;

    private Integer isDeleted;
}
