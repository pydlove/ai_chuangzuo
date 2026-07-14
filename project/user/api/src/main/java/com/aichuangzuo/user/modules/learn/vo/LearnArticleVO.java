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
    private String coverImageUrl;
    private ContentType contentType;
    private String content;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
    /** 上一篇，首篇为 null。 */
    private LearnArticleRefVO prevArticle;
    /** 下一篇，末篇为 null。 */
    private LearnArticleRefVO nextArticle;
}
