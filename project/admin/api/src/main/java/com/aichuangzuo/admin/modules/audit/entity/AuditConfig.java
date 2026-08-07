package com.aichuangzuo.admin.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 审计日志配置，对应表 a_audit_config。
 * 单行表，固定 id=1。
 */
@Getter
@Setter
@TableName("a_audit_config")
public class AuditConfig {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 日志保留天数。 */
    private Integer retentionDays;

    /** 清理任务 cron 表达式。 */
    private String cleanupCron;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
