package com.aichuangzuo.user.modules.learn.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.aichuangzuo.user.modules.learn.enums.ArticleStatus;
import com.aichuangzuo.user.modules.learn.enums.ContentType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户端 - 创作学院文章只读实体，映射 {@code t_article}。
 */
@Data
@TableName("t_article")
public class LearnArticleEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String title;

    private String summary;

    private ContentType contentType;

    private String content;

    private ArticleStatus status;

    private Integer sort;

    private Long authorId;

    private LocalDateTime publishedAt;

    @TableLogic
    @TableField(select = false)
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Long createdBy;

    private Long updatedBy;
}
