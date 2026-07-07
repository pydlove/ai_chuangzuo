package com.aichuangzuo.admin.modules.leaderboard.mapper;

import com.aichuangzuo.admin.modules.leaderboard.vo.LeaderboardTop10VO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端榜单聚合查询 Mapper。
 */
@Mapper
public interface LeaderboardAggregateMapper {

    List<LeaderboardTop10VO> selectCoinRankingMonth(@Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end,
                                                      @Param("limit") int limit);

    List<LeaderboardTop10VO> selectIncomeRankingMonth(@Param("periodMonth") String periodMonth,
                                                       @Param("limit") int limit);
}
