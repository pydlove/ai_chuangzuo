package com.aichuangzuo.admin.modules.generation.dto;

import lombok.Data;

/**
 * 「某任务累计 token 消耗」聚合结果（sumTokensByTaskIds 用）。
 */
@Data
public class TaskTokenSum {
    private Long taskId;
    private Long totalTokens;
}
