package com.aichuangzuo.admin.modules.generation.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GenerationTaskAdminVO {
    private Long id;
    private String bizNo;
    private Long userId;
    private String userNickname;
    /** 状态文本：queued / processing / completed / failed。 */
    private Integer status;
    private String statusLabel;
    private Long modelConfigId;
    private Integer wordLimitTarget;
    private Integer retryCount;
    private String lockedBy;
    private LocalDateTime lockedAt;
    private LocalDateTime leaseUntil;
    private String failedReason;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    /** 排队/执行已等待秒数（便于「执行中」「排对中」tab 看积压）。 */
    private Long waitingSeconds;
    /** 失败秒数（failed tab 距 now 秒数）。 */
    private Long failedSecondsAgo;
    /** 该任务累计 token 消耗（sum of successful AI calls；未产生调用为 0）。 */
    private Long totalTokens;
}
