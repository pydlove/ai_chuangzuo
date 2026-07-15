package com.aichuangzuo.user.modules.benefit.mapper;

import com.aichuangzuo.user.modules.benefit.entity.PlanBenefit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 套餐权益值 Mapper。
 */
@Mapper
public interface PlanBenefitMapper extends BaseMapper<PlanBenefit> {

    /**
     * 查询某套餐的全部权益值。
     *
     * @param planKey 套餐 key
     * @return 该套餐的权益值列表
     */
    @Select("SELECT * FROM u_plan_benefit WHERE plan_key = #{planKey}")
    List<PlanBenefit> selectByPlanKey(@Param("planKey") String planKey);
}
