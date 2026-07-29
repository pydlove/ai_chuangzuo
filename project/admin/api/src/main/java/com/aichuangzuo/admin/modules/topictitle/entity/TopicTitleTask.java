package com.aichuangzuo.admin.modules.topictitle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * AI 生成标题异步任务实体，对应表 {@code t_topic_title_task}。
 *
 * <p>状态机：0=queued, 1=processing, 2=completed, 3=failed（与 t_generation_task 对齐）。
 */
@Getter
@Setter
@TableName("t_topic_title_task")
public class TopicTitleTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 0=queued, 1=processing, 2=completed, 3=failed。 */
    private Integer status;

    /** 请求生成数量。 */
    private Integer count;

    /** 方向提示词（可选）。 */
    private String direction;

    /** 实际入库条数。 */
    private Integer generatedCount;

    /** 失败原因。 */
    private String failedReason;

    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime startedAt;
    private java.time.LocalDateTime completedAt;
}