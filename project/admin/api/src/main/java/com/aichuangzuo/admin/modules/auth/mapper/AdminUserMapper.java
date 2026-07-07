package com.aichuangzuo.admin.modules.auth.mapper;

import com.aichuangzuo.admin.modules.auth.entity.AdminUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {

    @Select("SELECT * FROM a_admin_user WHERE username = #{username} AND is_deleted = 0 LIMIT 1")
    AdminUser selectByUsername(@Param("username") String username);
}
