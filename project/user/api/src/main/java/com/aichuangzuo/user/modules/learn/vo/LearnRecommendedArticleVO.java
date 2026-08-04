package com.aichuangzuo.user.modules.learn.vo;

import lombok.Data;

/**
 * 创作学院 - 推荐文章卡片 VO。
 */
@Data
public class LearnRecommendedArticleVO {

    private Long id;
    private String title;
    private String summary;
    private String coverImageUrl;
    private String categoryName;
}
