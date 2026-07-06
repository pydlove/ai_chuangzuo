package com.aichuangzuo.admin.modules.auth.mapper;

import com.aichuangzuo.admin.modules.auth.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT * FROM a_role WHERE role_code = #{roleCode} AND is_deleted = 0 LIMIT 1")
    Role selectByRoleCode(@Param("roleCode") String roleCode);
}
