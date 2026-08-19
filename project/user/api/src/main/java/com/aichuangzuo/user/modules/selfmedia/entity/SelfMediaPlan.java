package com.aichuangzuo.user.modules.selfmedia.entity;

import com.aichuangzuo.shared.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("u_self_media_plan")
public class SelfMediaPlan extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String platformKey;
    private String platformName;
    private String goal;
    private String background;
    private Integer hasProduct;
    private String productDesc;
    private String nicheKey;
    private String nicheName;
    private String personaKey;
    private String personaName;
    private String contentPillarsJson;
    private String recommendationContextJson;
    private Integer isRecommendedByAi;
    private Long tenantId;
}
