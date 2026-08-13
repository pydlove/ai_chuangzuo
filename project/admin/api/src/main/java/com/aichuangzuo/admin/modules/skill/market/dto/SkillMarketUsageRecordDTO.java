package com.aichuangzuo.admin.modules.skill.market.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 提示词市场使用记录行（来自 a_generation_task）。
 */
@Data
public class SkillMarketUsageRecordDTO {
    private Long userId;
    private String userNickname;
    private String taskBizNo;
    private String articleBizNo;
    private LocalDateTime completedAt;
}
