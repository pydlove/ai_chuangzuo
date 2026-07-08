package com.aichuangzuo.user.modules.style.market.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户端 - 风格市场实体，映射 {@code u_style_market}。
 */
@Data
@TableName("u_style_market")
public class StyleMarket {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizNo;

    private String styleName;

    private String description;

    private String promptSummary;

    private String prompt;

    private String scope;

    private Long publisherUserId;

    private BigDecimal price;

    private Integer totalUses;

    private Integer weeklyUses;

    private BigDecimal weeklyEarnings;

    private BigDecimal milestoneBonus;

    private LocalDateTime lastSettlementAt;

    private Integer enableStatus;

    private Integer auditStatus;

    private Integer sourceType;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Long createdBy;

    private Long updatedBy;
}
