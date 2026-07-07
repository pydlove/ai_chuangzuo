package com.aichuangzuo.admin.modules.leaderboard.mapper;

import com.aichuangzuo.admin.modules.leaderboard.entity.RewardRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 榜单奖励发放记录 Mapper。
 */
@Mapper
public interface RewardRecordMapper extends BaseMapper<RewardRecord> {

    /**
     * 判断某用户在某周期某榜单是否已发放奖励。
     */
    @Select("SELECT COUNT(*) FROM u_leaderboard_reward_record " +
            "WHERE leaderboard_type = #{leaderboardType} AND period_month = #{periodMonth} " +
            "AND user_id = #{userId} AND is_deleted = 0")
    boolean exists(@Param("leaderboardType") Integer leaderboardType,
                   @Param("periodMonth") String periodMonth,
                   @Param("userId") Long userId);
}
