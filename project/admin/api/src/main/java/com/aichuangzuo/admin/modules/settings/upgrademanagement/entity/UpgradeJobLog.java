package com.aichuangzuo.admin.modules.settings.upgrademanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 升级脚本执行日志，对应表 {@code a_upgrade_job_log}。
 */
@Getter
@Setter
@TableName("a_upgrade_job_log")
public class UpgradeJobLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 脚本相对根目录路径。 */
    private String scriptRelativePath;

    /** 脚本文件名。 */
    private String scriptName;

    /** 触发方式：manual。 */
    private String triggerType;

    /** 状态：running/success/failed/timeout。 */
    private String runStatus;

    /** 开始时间。 */
    private LocalDateTime startedAt;

    /** 结束时间。 */
    private LocalDateTime finishedAt;

    /** 退出码。 */
    private Integer exitCode;

    /** 标准输出。 */
    private String stdout;

    /** 标准错误。 */
    private String stderr;

    /** 输出是否被截断：0-否，1-是。 */
    private Integer outputTruncated;

    private Long createdBy;
    private LocalDateTime createdAt;
    private Integer isDeleted;
}
