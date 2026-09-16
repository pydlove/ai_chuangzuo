package com.aichuangzuo.admin.modules.skill.market.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 全局提示词使用记录 VO。
 */
@Data
public class SkillUsageRecordVO {
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
