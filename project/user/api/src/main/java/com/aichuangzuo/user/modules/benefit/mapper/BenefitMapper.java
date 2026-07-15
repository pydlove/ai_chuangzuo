package com.aichuangzuo.user.modules.benefit.mapper;

import com.aichuangzuo.user.modules.benefit.entity.Benefit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 权益定义 Mapper。
 */
@Mapper
public interface BenefitMapper extends BaseMapper<Benefit> {

    /**
     * 根据编码查询启用中的权益。
     *
     * @param code 权益编码
     * @return 权益定义；不存在或已停用返回 null
     */
    @Select("SELECT * FROM u_benefit WHERE code = #{code} AND status = 1 LIMIT 1")
    Benefit selectByCode(@Param("code") String code);
}
