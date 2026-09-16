package com.aichuangzuo.admin.modules.skill.market.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 全局提示词使用记录行（来自 a_generation_task，跨市场/个人提示词）。
 */
@Data
public class SkillUsageRecordRowDTO {
    private String skillName;
    private String skillRef;
    private String articleTitle;
    private String articleBizNo;
    private Long userId;
    private String userNickname;
    private Long publisherUserId;
    private String publisherNickname;
    private LocalDateTime completedAt;
}
