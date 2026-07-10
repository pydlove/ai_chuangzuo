package com.aichuangzuo.admin.modules.learn.entity;

import com.aichuangzuo.admin.modules.learn.enums.ArticleStatus;
import com.aichuangzuo.admin.modules.learn.enums.ContentType;
import com.aichuangzuo.shared.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 创作学院文章实体；继承 {@link BaseEntity} 自动获得审计字段 + is_deleted 软删除。
 */
@Getter
@Setter
@TableName("t_article")
public class LearnArticleEntity extends BaseEntity {

    private Long categoryId;
    private String title;
    private String summary;
    private ContentType contentType;
    private String content;
    private ArticleStatus status;
    private Integer sort;
    private Long authorId;
    private LocalDateTime publishedAt;
}
