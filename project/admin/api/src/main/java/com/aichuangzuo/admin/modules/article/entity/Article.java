package com.aichuangzuo.admin.modules.article.entity;

import com.aichuangzuo.shared.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 管理端对用户作品表 {@code u_article} 的读模型。
 *
 * <p>表中不含 {@code created_by} / {@code updated_by} 列，因此覆盖基类同名字段，避免 MyBatis-Plus 生成不存在的列。
 */
@Getter
@Setter
@TableName("u_article")
public class Article extends BaseEntity {

    /** 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务唯一编号。 */
    private String bizNo;

    /** 所属用户ID。 */
    private Long userId;

    /** 作品标题。 */
    private String title;

    /** 作品正文。 */
    private String body;

    /** 发布描述。 */
    private String description;

    /** 推荐标签，JSON 数组字符串。 */
    private String tagsJson;

    /** AI 优化标题缓存，JSON 对象字符串。 */
    private String optimizedTitlesJson;

    /** 编辑器内联样式覆盖，JSON 字符串。 */
    private String styleOverrides;

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

    /** 表中无此列，覆盖基类字段。 */
    @TableField(exist = false)
    private Long createdBy;

    /** 表中无此列，覆盖基类字段。 */
    @TableField(exist = false)
    private Long updatedBy;
}
