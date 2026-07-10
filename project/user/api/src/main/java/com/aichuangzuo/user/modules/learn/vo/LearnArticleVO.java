package com.aichuangzuo.user.modules.learn.vo;

import com.aichuangzuo.user.modules.learn.enums.ContentType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LearnArticleVO {
    private Long id;
    private Long categoryId;
    private String title;
    private String summary;
    private ContentType contentType;
    private String content;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
}
