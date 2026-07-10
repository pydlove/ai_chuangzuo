package com.aichuangzuo.admin.modules.generation.dto.request;

import lombok.Data;

/**
 * 创作任务列表查询请求。
 *
 * <p>{@code status} 可选；null 时查全部 in-flight 状态（QUEUED / PROCESSING / FAILED）。
 */
@Data
public class GenerationTaskQueryRequest {

    /** 状态：0-queued / 1-processing / 2-completed / 3-failed；null=查全部非完成。 */
    private Integer status;

    /** 关键字：bizNo / 用户昵称。 */
    private String keyword;

    private long page = 1;
    private long pageSize = 20;
}
