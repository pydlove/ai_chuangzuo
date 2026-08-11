package com.aichuangzuo.shared.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 分享配置实体，对应表 {@code u_share_config}。
 *
 * <p>两端共享实体：admin-api 负责配置管理；user-api 负责按场景查询并渲染给用户端。</p>
 */
@Getter
@Setter
@TableName("u_share_config")
public class ShareConfig extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分享场景：lottery-抽奖活动，invite-邀请有礼。 */
    private String sceneKey;

    /** 配置标题（管理端展示用）。 */
    private String title;

    /** 分享文案，支持占位符。 */
    private String content;

    /** 是否启用：0-禁用，1-启用。 */
    private Integer enabled;

    /** 排序，越小越靠前。 */
    private Integer sortOrder;
}
