package com.aichuangzuo.admin.modules.benefit.mapper;

import com.aichuangzuo.admin.modules.benefit.entity.BenefitUsageAggregate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户额度用量管理 Mapper。
 */
@Mapper
public interface BenefitUsageAdminMapper extends BaseMapper<BenefitUsageAggregate> {

    /**
     * 将指定周期内指定权益的 used_count 和 pre_used_count 清零。
     * 用于管理员针对故障导致额度被占用的兜底重置。
     *
     * @param userId 用户ID
     * @param benefitCode 权益编码
     * @param period 周期
     * @return 受影响行数
     */
    @Update("UPDATE u_benefit_usage SET used_count = 0, pre_used_count = 0 " +
            "WHERE user_id = #{userId} AND benefit_code = #{benefitCode} AND period = #{period}")
    int resetQuotaByPeriod(@Param("userId") Long userId,
                           @Param("benefitCode") String benefitCode,
                           @Param("period") String period);

    /**
     * 原子地将指定周期内指定权益的 used_count 减少指定数量，下限为 0。
     * 用于管理员释放用户自定义提示词额度。
     *
     * @param userId 用户ID
     * @param benefitCode 权益编码
     * @param period 周期
     * @param count 释放数量
     * @return 受影响行数
     */
    @Update("UPDATE u_benefit_usage SET used_count = GREATEST(used_count - #{count}, 0) " +
            "WHERE user_id = #{userId} AND benefit_code = #{benefitCode} AND period = #{period} AND used_count > 0")
    int decreaseUsedCount(@Param("userId") Long userId,
                          @Param("benefitCode") String benefitCode,
                          @Param("period") String period,
                          @Param("count") int count);
}
