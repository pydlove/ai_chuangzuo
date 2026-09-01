package com.aichuangzuo.user.modules.skill.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户风格市场收藏实体，映射 {@code u_user_market_favorite}。
 */
@Getter
@Setter
@TableName("u_user_market_favorite")
public class UserMarketFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String marketSkillId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
