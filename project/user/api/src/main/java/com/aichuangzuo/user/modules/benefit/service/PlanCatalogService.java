package com.aichuangzuo.user.modules.benefit.service;

import com.aichuangzuo.user.modules.benefit.vo.NewcomerOfferVO;
import com.aichuangzuo.user.modules.benefit.vo.PlanCatalogVO;

/**
 * 公开接口：组装定价页所需的套餐目录。
 */
public interface PlanCatalogService {

    /** 查询完整目录（带缓存）。 */
    PlanCatalogVO getCatalog();

    /** 查询当前用户的新人首冲优惠（需登录）。 */
    NewcomerOfferVO getNewcomerOffer(Long userId);
}