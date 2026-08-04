package com.aichuangzuo.admin.modules.user.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端用户收藏提示词视图。
 */
@Getter
@Setter
public class AdminUserFavoriteSkillVO {

    /** 市场提示词业务编号。 */
    private String bizNo;

    /** 提示词名称。 */
    private String skillName;

    /** 提示词摘要。 */
    private String promptSummary;

    /** 提示词内容。 */
    private String prompt;

    /** 适用范围。 */
    private String scope;

    /** 单次使用价格。 */
    private BigDecimal price;

    /** 发布者邮箱。 */
    private String publisherEmail;

    /** 发布者昵称。 */
    private String publisherNickname;

    /** 审核状态：0-待审核，1-已通过。 */
    private Integer auditStatus;

    /** 收藏时间。 */
    private LocalDateTime favoriteAt;
}
