package com.aichuangzuo.admin.modules.generation.vo;

import lombok.Data;

import java.util.List;

/**
 * 批量停止创作任务结果。
 */
@Data
public class BatchStopGenerationTaskResultVO {

    /** 提交的任务数。 */
    private int total;

    /** 成功停止数。 */
    private int successCount;

    /** 不存在的任务 ID。 */
    private List<Long> missingIds;

    /** 状态不允许停止的任务 ID。 */
    private List<Long> invalidIds;

    /** 停止过程中发生异常的任务 ID。 */
    private List<Long> failedIds;
}
