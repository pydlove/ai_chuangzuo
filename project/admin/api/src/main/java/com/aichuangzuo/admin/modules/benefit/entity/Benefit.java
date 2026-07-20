package com.aichuangzuo.admin.modules.benefit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 权益定义（对应 u_benefit，跨端共享）。
 * 管理端只读 + 改展示相关字段（display_label / card_value_tpl / value_label_json），
 * 类型与 code 不允许修改以免破坏已有业务逻辑。
 */
@Getter
@Setter
@TableName("u_benefit")
public class Benefit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private String type;
    private String description;
    private String displayLabel;
    private String cardValueTpl;
    private String valueLabelJson;
    private Integer sortOrder;
    private Integer status;
}