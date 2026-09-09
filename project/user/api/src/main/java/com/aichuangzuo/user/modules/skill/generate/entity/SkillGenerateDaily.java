package com.aichuangzuo.user.modules.skill.generate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户 AI 提示词帮写日次数统计，对应表 {@code u_skill_generate_daily}。
 */
@Getter
@Setter
@TableName("u_skill_generate_daily")
public class SkillGenerateDaily {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID。 */
    private Long userId;

    /** 帮写日期。 */
    private LocalDate attemptDate;

    /** 当日已帮写次数。 */
    private Integer attemptCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private Long tenantId;
}
