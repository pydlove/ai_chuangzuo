package com.aichuangzuo.user.modules.membership.service;

import com.aichuangzuo.user.modules.membership.entity.Plan;

/**
 * 套餐元数据查询（价格、显示名）。结果缓存 10 分钟。
 */
public interface PlanLookupService {

    /** 按 key 查询（带缓存）；未命中或停用返回 null。 */
    Plan findActive(String planKey);

    /** 套餐显示名；未命中时返回 planKey。 */
    String getDisplayName(String planKey);
}