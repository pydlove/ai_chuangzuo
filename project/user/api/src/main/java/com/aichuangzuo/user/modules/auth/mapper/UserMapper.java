package com.aichuangzuo.user.modules.auth.mapper;

import com.aichuangzuo.user.modules.auth.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM u_user WHERE email = #{email} AND is_deleted = 0 LIMIT 1")
    User selectByEmail(String email);

    @Select("SELECT * FROM u_user WHERE invite_code = #{inviteCode} AND is_deleted = 0 LIMIT 1")
    User selectByInviteCode(String inviteCode);

    @Update("UPDATE u_user SET password_hash = #{hash}, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int updatePassword(@Param("id") Long id, @Param("hash") String hash);
}
