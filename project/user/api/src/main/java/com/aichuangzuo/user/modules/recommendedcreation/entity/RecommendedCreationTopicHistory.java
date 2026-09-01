package com.aichuangzuo.user.modules.recommendedcreation.entity;

import com.aichuangzuo.shared.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 小爱推荐创作选题历史记录实体，对应表 {@code u_recommended_creation_topic_history}。
 *
 * <p>用于记录每次为用户生成的选题标题，以便后续两周内去重。</p>
 */
@Getter
@Setter
@TableName("u_recommended_creation_topic_history")
public class RecommendedCreationTopicHistory extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户ID。 */
    private Long userId;

    /** 已推荐选题标题。 */
    private String title;

    /** 关联的推荐创作会话ID。 */
    private Long sessionId;

    /** 租户ID。 */
    private Long tenantId;
}
