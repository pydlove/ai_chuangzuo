package com.aichuangzuo.admin.modules.stats.mapper;

import lombok.Data;

import java.math.BigDecimal;

/** 看板分组统计行：itemKey=分组键（日期或类别），itemCount=计数，itemAmount=金额（无金额场景为 null） */
@Data
public class DashboardGroupRow {

    /** 分组键：yyyy-MM-dd 日期，或套餐/平台 key */
    private String itemKey;

    /** 计数 */
    private Long itemCount;

    /** 金额（元） */
    private BigDecimal itemAmount;
}
