package com.aichuangzuo.admin.modules.planbenefit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 套餐权益值（对应 u_plan_benefit，跨端共享）。
 */
@Getter
@Setter
@TableName("u_plan_benefit")
public class PlanBenefit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String planKey;
    private String benefitCode;
    private String benefitValue;
}