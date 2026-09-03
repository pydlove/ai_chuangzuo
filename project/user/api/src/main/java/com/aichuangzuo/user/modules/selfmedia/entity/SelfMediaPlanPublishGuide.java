package com.aichuangzuo.user.modules.selfmedia.entity;

import com.aichuangzuo.shared.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("u_self_media_plan_publish_guide")
public class SelfMediaPlanPublishGuide extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long planId;
    private LocalDateTime planUpdatedAt;
    private String planContentHash;
    private String mainPlatform;
    private String mainPlatformJson;
    private String coldStartJson;
    private String repostsJson;
    private Long tenantId;
}
