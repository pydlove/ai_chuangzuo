package com.aichuangzuo.user.modules.recommendedcreation.entity;

import com.aichuangzuo.shared.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("u_recommended_creation_session")
public class RecommendedCreationSession extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Integer currentStep;
    private String topicsJson;
    private String selectedTopicJson;
    private String anglesJson;
    private String selectedAnglesJson;
    private Integer wordCount;
    private String prompt;
    private String template;
    private String status;
    private Long tenantId;
}
