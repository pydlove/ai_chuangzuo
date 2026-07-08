package com.aichuangzuo.user.modules.membership.mapper;

import com.aichuangzuo.user.modules.membership.entity.UserMembership;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户会员状态 Mapper。
 */
@Mapper
public interface UserMembershipMapper extends BaseMapper<UserMembership> {

    /**
     * 根据用户 ID 查询会员状态。
     *
     * @param userId 用户ID
     * @return 会员状态；未开通返回 null
     */
    @Select("SELECT * FROM u_user_membership WHERE user_id = #{userId} LIMIT 1")
    UserMembership selectByUserId(@Param("userId") Long userId);
}
