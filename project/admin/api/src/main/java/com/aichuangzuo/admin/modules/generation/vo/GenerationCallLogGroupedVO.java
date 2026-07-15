package com.aichuangzuo.admin.modules.generation.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 创作任务 AI 调用日志的「按 stage 分组」查询结果包装。
 * 顺带返回任务当前 status，方便前端决定是否继续轮询。
 */
@Data
public class GenerationCallLogGroupedVO {

    /** 任务当前 status（0=queued, 1=processing, 2=completed, 3=failed）；任务不存在时为 null。 */
    private Integer taskStatus;

    /** key=stageIndex, value=该 stage 的所有 attempt 记录。 */
    private Map<Integer, List<GenerationCallLogVO>> grouped;
}