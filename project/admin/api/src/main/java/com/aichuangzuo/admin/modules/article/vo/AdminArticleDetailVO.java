package com.aichuangzuo.admin.modules.article.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 管理端用户作品详情视图。
 */
@Getter
@Setter
public class AdminArticleDetailVO {

    /** 业务编号。 */
    private String bizNo;

    /** 用户ID。 */
    private Long userId;

    /** 作品标题。 */
    private String title;

    /** 作品正文。 */
    private String body;

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

    /** 推荐标签 JSON 字符串。 */
    private String tagsJson;

    /** 编辑器内联样式覆盖 JSON 字符串。 */
    private String styleOverrides;

    /** 生成完成时间。 */
    private LocalDateTime completedAt;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
