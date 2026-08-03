package com.aichuangzuo.admin.modules.user.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 管理端用户提示词视图。
 */
@Getter
@Setter
public class AdminUserSkillVO {

    /** 业务编号。 */
    private String bizNo;

    /** 提示词名称。 */
    private String skillName;

    /** 提示词内容。 */
    private String prompt;

    /** 适用范围。 */
    private String scope;

    /** 来源类型：1-自定义，2-学习。 */
    private Integer sourceType;

    /** 使用次数。 */
    private Integer useCount;

    /** 审核状态：0-待审核，1-已通过，2-已拒绝。 */
    private Integer auditStatus;

    /** 创建时间。 */
    private LocalDateTime createdAt;
}
