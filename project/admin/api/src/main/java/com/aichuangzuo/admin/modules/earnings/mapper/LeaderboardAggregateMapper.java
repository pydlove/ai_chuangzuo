package com.aichuangzuo.admin.modules.earnings.mapper;

import com.aichuangzuo.admin.modules.earnings.vo.LeaderboardTop10VO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LeaderboardAggregateMapper {

    List<LeaderboardTop10VO> selectCoinTop10(@Param("month") String month);

    List<LeaderboardTop10VO> selectIncomeTop10(@Param("month") String month);
}
