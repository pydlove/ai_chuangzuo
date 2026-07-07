package com.aichuangzuo.user.modules.leaderboard.mapper;

import com.aichuangzuo.user.modules.leaderboard.vo.LeaderboardEntryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 榜单聚合查询 Mapper。
 */
@Mapper
public interface LeaderboardAggregateMapper {

    List<LeaderboardEntryVO> selectCoinRanking(@Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end,
                                                 @Param("limit") int limit);

    LeaderboardEntryVO selectCoinAmountByUser(@Param("userId") Long userId,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    List<LeaderboardEntryVO> selectIncomeRankingMonth(@Param("periodMonth") String periodMonth,
                                                      @Param("limit") int limit);

    List<LeaderboardEntryVO> selectIncomeRankingYear(@Param("year") String year,
                                                     @Param("limit") int limit);

    LeaderboardEntryVO selectIncomeAmountByUserMonth(@Param("userId") Long userId,
                                                     @Param("periodMonth") String periodMonth);

    LeaderboardEntryVO selectIncomeAmountByUserYear(@Param("userId") Long userId,
                                                    @Param("year") String year);
}
