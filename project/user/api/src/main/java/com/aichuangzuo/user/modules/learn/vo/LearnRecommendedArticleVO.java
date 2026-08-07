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
    /** 是否免费：1=免费，0=付费 */
    private Integer isFree;
    /** 最低所需套餐 key（仅付费时有值） */
    private String requiredPlanKey;
    /** 最低所需套餐展示名（仅付费时有值） */
    private String requiredPlanName;
    /** 当前用户是否可读完整正文：服务端按登录态 + 套餐等级综合判断 */
    private Boolean canRead;
}
