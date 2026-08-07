package com.aichuangzuo.admin.modules.learn.entity;

import com.aichuangzuo.admin.modules.learn.enums.ArticleStatus;
import com.aichuangzuo.admin.modules.learn.enums.ContentType;
import com.aichuangzuo.shared.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;
    private String title;
    private String summary;
    private String coverImageUrl;
    private ContentType contentType;
    private String content;
    private ArticleStatus status;
    private Integer sort;

    /** 是否推荐：0=否 1=是 */
    private Integer isRecommended;

    /** 是否免费：1=免费，0=付费。历史数据默认 1。 */
    private Integer isFree;

    /** 最低所需套餐 key（仅付费时有值），值见 MembershipPlan#getKey() */
    private String requiredPlanKey;

    private Long authorId;
    private LocalDateTime publishedAt;
}
