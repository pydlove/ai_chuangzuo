package com.aichuangzuo.admin.modules.user.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 用户学习提示词按月统计视图。
 */
@Getter
@Setter
public class AdminLearnedSkillMonthVO {

    /** 周期，格式 yyyy-MM。 */
    private String period;

    /** 当月学习次数（used_count）。 */
    private Integer usedCount;

    /** 当月预扣次数（pre_used_count）。 */
    private Integer preUsedCount;

    /** 当月学习产生的提示词数量。 */
    private Long skillCount;
}
