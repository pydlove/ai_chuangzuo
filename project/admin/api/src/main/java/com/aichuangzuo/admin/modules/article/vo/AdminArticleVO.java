package com.aichuangzuo.admin.modules.article.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 管理端用户作品列表视图。
 */
@Getter
@Setter
public class AdminArticleVO {

    /** 业务编号。 */
    private String bizNo;

    /** 用户ID。 */
    private Long userId;

    /** 作品标题。 */
    private String title;

    /** 作品描述/摘要。 */
    private String description;

    /** 目标平台。 */
    private String platform;

    /** 使用风格。 */
    private String skill;

    /** 使用模板。 */
    private String template;

    /** 字数。 */
    private Integer wordCount;

    /** 生成完成时间。 */
    private LocalDateTime completedAt;

    /** 创建时间。 */
    private LocalDateTime createdAt;
}
