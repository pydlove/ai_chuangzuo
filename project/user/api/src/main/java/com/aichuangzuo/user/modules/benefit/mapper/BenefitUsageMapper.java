package com.aichuangzuo.user.modules.benefit.mapper;

import com.aichuangzuo.user.modules.benefit.entity.BenefitUsage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 权益用量 Mapper。
 */
@Mapper
public interface BenefitUsageMapper extends BaseMapper<BenefitUsage> {

    /**
     * 查询用户某权益当前周期的用量记录。
     *
     * @param userId 用户ID
     * @param benefitCode 权益编码
     * @param period 周期标识
     * @return 用量记录；未消费过返回 null
     */
    @Select("SELECT * FROM u_benefit_usage WHERE user_id = #{userId} AND benefit_code = #{benefitCode} AND period = #{period} LIMIT 1")
    BenefitUsage selectByUserAndCodeAndPeriod(@Param("userId") Long userId,
                                              @Param("benefitCode") String benefitCode,
                                              @Param("period") String period);

    /**
     * 原子地将用量 +1，仅在未超限时生效（防并发超额）。
     *
     * @param userId 用户ID
     * @param benefitCode 权益编码
     * @param period 周期标识
     * @param limit 额度上限
     * @return 受影响行数；0 表示记录不存在或已达上限
     */
    @Update("UPDATE u_benefit_usage SET used_count = used_count + 1 " +
            "WHERE user_id = #{userId} AND benefit_code = #{benefitCode} AND period = #{period} AND used_count < #{limit}")
    int incrementIfBelowLimit(@Param("userId") Long userId,
                              @Param("benefitCode") String benefitCode,
                              @Param("period") String period,
                              @Param("limit") int limit);
}
