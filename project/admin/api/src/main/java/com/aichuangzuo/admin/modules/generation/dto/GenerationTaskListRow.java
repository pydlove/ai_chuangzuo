package com.aichuangzuo.admin.modules.generation.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Admin 端-创作任务列表行（含用户昵称），由 mapper 自定义 SQL 返回。
 */
@Data
public class GenerationTaskListRow {
    private Long id;
    private String bizNo;
    private Long userId;
    private String userNickname;
    private Integer status;
    private Long modelConfigId;
    private Integer wordLimitTarget;
    private Integer retryCount;
    private String lockedBy;
    private LocalDateTime lockedAt;
    private LocalDateTime leaseUntil;
    private String failedReason;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
