package com.aichuangzuo.admin.modules.settings.upgrademanagement.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 升级脚本执行日志 VO。
 */
@Data
public class UpgradeJobLogVO {

    private Long id;
    private String scriptRelativePath;
    private String scriptName;
    private String triggerType;
    private String runStatus;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer exitCode;
    private String stdout;
    private String stderr;
    private Boolean outputTruncated;
    private Long createdBy;
    private LocalDateTime createdAt;
}
