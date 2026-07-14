package com.aichuangzuo.admin.modules.learn.vo;

import com.aichuangzuo.admin.modules.learn.enums.ArticleStatus;
import com.aichuangzuo.admin.modules.learn.enums.ContentType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LearnArticleDetail {

    private Long id;
    private Long categoryId;
    private String title;
    private String summary;
    private String coverImageUrl;
    private ContentType contentType;
    private String content;
    private ArticleStatus status;
    private Integer sort;
    private Long authorId;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
