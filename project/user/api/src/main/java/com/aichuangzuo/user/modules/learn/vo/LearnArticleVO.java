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
    /** 是否免费：1=免费，0=付费 */
    private Integer isFree;
    /** 最低所需套餐 key（仅付费时有值） */
    private String requiredPlanKey;
    /** 最低所需套餐展示名（仅付费时有值） */
    private String requiredPlanName;
    /** 当前用户是否可读完整正文：服务端按登录态 + 套餐等级综合判断 */
    private Boolean canRead;
    /** 上一篇，首篇为 null。 */
    private LearnArticleRefVO prevArticle;
    /** 下一篇，末篇为 null。 */
    private LearnArticleRefVO nextArticle;
}
