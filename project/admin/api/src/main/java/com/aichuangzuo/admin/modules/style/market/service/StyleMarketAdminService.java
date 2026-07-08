package com.aichuangzuo.admin.modules.style.market.service;

import com.aichuangzuo.admin.modules.style.market.dto.request.CreateStyleMarketRequest;
import com.aichuangzuo.admin.modules.style.market.dto.request.StyleMarketPageRequest;
import com.aichuangzuo.admin.modules.style.market.dto.request.UpdateStyleMarketRequest;
import com.aichuangzuo.admin.modules.style.market.vo.StyleMarketVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 管理端 - 风格市场服务。
 */
public interface StyleMarketAdminService {

    /**
     * 分页查询风格市场列表。
     */
    IPage<StyleMarketVO> page(StyleMarketPageRequest request);

    /**
     * 创建风格市场条目，返回新生成的 bizNo。
     */
    String create(CreateStyleMarketRequest request);

    /**
     * 更新风格市场条目（全量字段）。
     */
    void update(String bizNo, UpdateStyleMarketRequest request);

    /**
     * 软删除（is_deleted=1）。
     */
    void delete(String bizNo);
}
