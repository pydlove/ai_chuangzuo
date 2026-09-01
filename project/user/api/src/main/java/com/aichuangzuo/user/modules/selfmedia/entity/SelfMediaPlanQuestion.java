package com.aichuangzuo.user.modules.selfmedia.entity;

import com.aichuangzuo.shared.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("u_self_media_plan_question")
public class SelfMediaPlanQuestion extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String platformKey;
    private String promptCode;
    private String questionKey;
    private String questionText;
    private String optionsJson;
    private Integer isRequired;
    private Integer sortOrder;
    private Integer allowOther;
    private Integer otherMaxLength;
}
