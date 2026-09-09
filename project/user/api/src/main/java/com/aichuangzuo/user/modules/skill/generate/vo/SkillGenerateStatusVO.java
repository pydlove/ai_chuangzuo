package com.aichuangzuo.user.modules.skill.generate.vo;

import lombok.Data;

/**
 * 小爱帮写状态（权限 + 当日剩余次数）。
 */
@Data
public class SkillGenerateStatusVO {

    /** 当前套餐是否可用（专业版及以上）。 */
    private Boolean allowed;

    /** 每日上限。 */
    private Integer dailyLimit;

    /** 今日剩余次数。 */
    private Integer remainingToday;
}
