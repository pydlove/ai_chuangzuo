package com.aichuangzuo.user.modules.selfmedia.entity;

import com.aichuangzuo.shared.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("u_self_media_plan_niche")
public class SelfMediaPlanNiche extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String platformKey;
    private String answerSnapshotHash;
    private String answerSnapshotJson;
    private String nicheKey;
    private String name;
    private String audience;
    private String monetization;
    private String riskLabel;
    private String riskColor;
    private Integer caseCount;
    private String reason;
}
