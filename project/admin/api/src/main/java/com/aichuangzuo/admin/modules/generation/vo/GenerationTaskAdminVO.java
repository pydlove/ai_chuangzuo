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
    /** 模型配置展示：名称/供应商，如 "默认key/MiniMax"。 */
    private String modelConfigDisplay;
    private Integer wordLimitTarget;
    private Integer retryCount;
    /** 用户提交的标题。 */
    private String title;
    /** 创作要求 / 描述。 */
    private String description;
    /** 目标平台。 */
    private String platform;
    /** 用户选择的 skill / 提示词引用。 */
    private String skillRef;
    /** 导出模板。 */
    private String template;
    /** 快照后的用户风格提示词。 */
    private String userSkillPrompt;
    private String lockedBy;
    private LocalDateTime lockedAt;
    private LocalDateTime leaseUntil;
    private String failedReason;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    /** 已完成任务关联的 u_article.biz_no；未完成时为空。 */
    private String articleBizNo;
    /** 排队/执行已等待秒数（便于「执行中」「排对中」tab 看积压）。 */
    private Long waitingSeconds;
    /** 失败秒数（failed tab 距 now 秒数）。 */
    private Long failedSecondsAgo;
    /** 该任务累计 token 消耗（sum of successful AI calls；未产生调用为 0）。 */
    private Long totalTokens;
}
