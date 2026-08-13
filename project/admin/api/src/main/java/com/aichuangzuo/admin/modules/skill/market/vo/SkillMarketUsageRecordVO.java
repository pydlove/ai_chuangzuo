package com.aichuangzuo.admin.modules.skill.market.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 提示词市场使用记录 VO。
 */
@Data
public class SkillMarketUsageRecordVO {
    private Long userId;
    private String userNickname;
    private String taskBizNo;
    private String articleBizNo;
    private LocalDateTime completedAt;
}
