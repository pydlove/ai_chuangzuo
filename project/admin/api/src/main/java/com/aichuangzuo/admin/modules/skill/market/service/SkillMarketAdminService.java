package com.aichuangzuo.admin.modules.skill.market.service;

import com.aichuangzuo.admin.modules.earnings.vo.PageResult;
import com.aichuangzuo.admin.modules.skill.market.vo.MarketSkillStatsVO;
import com.aichuangzuo.admin.modules.skill.market.vo.SkillMarketUsageRecordVO;
import com.aichuangzuo.admin.modules.skill.market.dto.request.CreateSkillMarketRequest;
import com.aichuangzuo.admin.modules.skill.market.dto.request.SkillMarketPageRequest;
import com.aichuangzuo.admin.modules.skill.market.dto.request.UpdateSkillMarketRequest;
import com.aichuangzuo.admin.modules.skill.market.vo.SkillMarketVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 管理端 - 风格市场服务。
 */
public interface SkillMarketAdminService {

    /**
     * 分页查询风格市场列表。
     */
    IPage<SkillMarketVO> page(SkillMarketPageRequest request);

    /**
     * 创建风格市场条目，返回新生成的 bizNo。
     */
    String create(CreateSkillMarketRequest request);

    /**
     * 更新风格市场条目（全量字段）。
     */
    void update(String bizNo, UpdateSkillMarketRequest request);

    /**
     * 软删除（is_deleted=1）。
     */
    void delete(String bizNo);

    /**
     * 提示词市场统计概览。
     */
    MarketSkillStatsVO stats();

    /**
     * 模拟一次提示词使用：指定用户消费一次该提示词，发布者获得收益。
     */
    void simulateUsage(String bizNo, Long userId);

    /**
     * 分页查询指定提示词的使用记录。
     */
    PageResult<SkillMarketUsageRecordVO> listUsageRecords(String bizNo, int pageNum, int pageSize);
}
