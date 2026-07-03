package com.aichuangzuo.user.modules.auth.mapper;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM u_user WHERE email = #{email} AND is_deleted = 0 LIMIT 1")
    User selectByEmail(String email);

    @Select("SELECT * FROM u_user WHERE invite_code = #{inviteCode} AND is_deleted = 0 LIMIT 1")
    User selectByInviteCode(String inviteCode);
}
