package com.aichuangzuo.shared.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 已归档的生成任务，对应表 {@code a_generation_history}（cold storage）。
 *
 * <p>字段裁剪：砍掉 updated_at / updated_by / is_deleted / retention_days / locked_by
 * （这些在归档后无意义），加 {@code taskId} 指向原 task。
 *
 * <p>正文（生成内容）不复制，正文仍在 {@code u_article}，按 {@code bizNo} 反查。
 */
@Getter
@Setter
@TableName("a_generation_history")
public class GenerationHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原 a_generation_task.id。 */
    private Long taskId;

    /** 业务唯一编号（同原 task）。 */
    private String bizNo;

    /** 发起任务的用户 ID。 */
    private Long targetUserId;

    /** 任务结束时已确定的 title（快照，跨完稿 / 失败）。 */
    private String title;

    /** 输入参数 JSON（同原 task）。 */
    private String inputParam;

    /** 终态：completed / failed。归档时只迁移这两个状态。 */
    private Integer status;

    /** 累计重试次数。 */
    private Integer retryCount;

    /** 失败原因（status=failed 时）。 */
    private String failedReason;

    /** 用户要求字数。 */
    private Integer wordLimitTarget;

    /** 使用的 AI 模型配置 ID。 */
    private Long modelConfigId;

    /** 使用的提示词模板 ID。 */
    private Long promptTemplateId;

    /** 原任务创建时间。 */
    private LocalDateTime createdAt;

    /** 完成时间。 */
    private LocalDateTime completedAt;

    /** 处理耗时（毫秒）。 */
    private Long durationMs;

    /** 入档时间。 */
    private LocalDateTime archivedAt;
}
