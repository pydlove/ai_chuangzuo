package com.aichuangzuo.user.modules.selfmedia.entity;

import com.aichuangzuo.shared.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("u_self_media_plan_persona")
public class SelfMediaPlanPersona extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String platformKey;
    private String answerSnapshotHash;
    private String nicheKey;
    private String personaKey;
    private String name;
    private String description;
    private String defaultPillarsJson;
}
