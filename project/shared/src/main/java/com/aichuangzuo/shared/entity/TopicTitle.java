package com.aichuangzuo.shared.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 选题标题库实体，对应表 {@code u_topic_title}。
 *
 * <p>两端共享实体：admin-api 生成 / 分页 / 逻辑删除；user-api 随机拉取 / 使用计数。
 * migration 由 user-api 拥有（V1.0.0_025）。
 */
@Getter
@Setter
@TableName("u_topic_title")
public class TopicTitle extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题。 */
    private String title;

    /** 标题概要（写作方向）。 */
    private String summary;

    /** 生成时用的方向提示词（追溯用），空串表示未指定。 */
    private String direction;

    /** 全站累计使用次数。 */
    private Integer useCount;

    /** 租户ID。 */
    private Long tenantId;
}
