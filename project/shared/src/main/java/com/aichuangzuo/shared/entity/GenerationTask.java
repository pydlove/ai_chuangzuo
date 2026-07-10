package com.aichuangzuo.shared.entity;

import com.aichuangzuo.shared.enums.GenerationTaskStatus;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文章生成任务实体，对应表 {@code a_generation_task}。
 *
 * <p>两端共享实体：user-api 写入（submit / retry）；admin-api worker 读取 / 更新状态。
 * migration 仅由 admin-api 拥有。
 */
@Getter
@Setter
@TableName("a_generation_task")
public class GenerationTask extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务唯一编号（对外暴露，例如 GA20260709xxxx）。 */
    private String bizNo;

    /** 发起任务的用户 ID（u_user.id）。 */
    private Long targetUserId;

    /** 任务状态。 */
    @EnumValue
    private GenerationTaskStatus status;

    /** 使用的 AI 模型配置 ID（a_model_config.id）。 */
    private Long modelConfigId;

    /** 使用的提示词模板 ID（提交时快照，t_prompt_template.id）。 */
    private Long promptTemplateId;

    /**
     * 任务创建时锁定的模板版本号（V2.0.0_019 新增）。
     *
     * <p>NULL 表示老任务（V2.0.0_019 之前的任务），runtime 走 fallback：
     * PipelineTemplateResolver 解析时使用当前 enabled=1 的最新版。
     */
    private Integer promptTemplateVersion;

    /**
     * 输入参数 JSON，包含：
     * <pre>
     *   { "title": "...", "description": "...", "platform": "wechat",
     *     "styleRef": "uuid", "wordCount": 1500, "toneTags": [...] }
     * </pre>
     */
    private String inputParam;

    /** 用户要求字数（≤ 3000）。 */
    private Integer wordLimitTarget;

    /** 已重试次数。 */
    private Integer retryCount;

    /** 最大重试次数，默认 3。 */
    private Integer maxRetry;

    /** worker 锁定（开始处理）时间。IGNORE_NULL：允许 set null 时写入 SQL。 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private LocalDateTime lockedAt;

    /** worker 实例 ID。 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String lockedBy;

    /** lease 超时时刻。 */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private LocalDateTime leaseUntil;

    /** 最后一次失败原因。 */
    private String failedReason;

    /** 完成时间（成功 / 最终失败）。 */
    private LocalDateTime completedAt;

    /** 保留天数（null = 永久）。 */
    private Integer retentionDays;

    /** 租户 ID（=0）。 */
    private Long tenantId;
}
