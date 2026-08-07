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
    private Integer isRecommended;
    /** 是否免费：1=免费，0=付费 */
    private Integer isFree;
    /** 最低所需套餐 key（仅付费时有值） */
    private String requiredPlanKey;
    private Long authorId;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
