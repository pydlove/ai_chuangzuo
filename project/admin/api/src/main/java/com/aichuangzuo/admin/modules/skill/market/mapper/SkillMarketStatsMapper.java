package com.aichuangzuo.admin.modules.skill.market.mapper;

import com.aichuangzuo.admin.modules.skill.market.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 提示词市场统计与使用记录 Mapper。
 */
@Mapper
public interface SkillMarketStatsMapper {

    SkillMarketOverviewDTO selectOverview();

    List<SkillMarketTopSkillDTO> selectTopSkillsByTotalUses(@Param("limit") int limit);

    List<SkillMarketTopPublisherDTO> selectTopPublishersByWeeklyEarnings(@Param("limit") int limit);

    List<SkillMarketTrendDTO> selectUsageTrend(@Param("days") int days);

    List<SkillMarketUsageRecordDTO> selectUsageRecords(@Param("skillRef") String skillRef,
                                                       @Param("offset") long offset,
                                                       @Param("limit") int limit);

    long countUsageRecords(@Param("skillRef") String skillRef);

    /** 全局使用记录：跨市场/个人提示词，含文章标题与作者。 */
    List<SkillUsageRecordRowDTO> selectGlobalUsageRecords(@Param("keyword") String keyword,
                                                          @Param("offset") long offset,
                                                          @Param("limit") int limit);

    long countGlobalUsageRecords(@Param("keyword") String keyword);
}
