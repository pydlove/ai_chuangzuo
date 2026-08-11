package com.aichuangzuo.admin.modules.generation.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 批量停止创作任务请求。
 */
@Data
public class BatchStopGenerationTaskRequest {

    /** 待停止任务 ID 列表；仅允许停止 status=QUEUED/PROCESSING 的任务。 */
    private List<Long> ids;
}
