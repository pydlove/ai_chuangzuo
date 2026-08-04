package com.aichuangzuo.user.modules.skill.analyze.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户 AI 提示词分析日次数统计，对应表 {@code u_skill_analyze_daily}。
 */
@Getter
@Setter
@TableName("u_skill_analyze_daily")
public class SkillAnalyzeDaily {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID。 */
    private Long userId;

    /** 分析日期。 */
    private LocalDate attemptDate;

    /** 当日已分析次数。 */
    private Integer attemptCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private Long tenantId;
}
