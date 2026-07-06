package com.aichuangzuo.admin.modules.auth.mapper;

import com.aichuangzuo.admin.modules.auth.entity.AdminUserRoleRel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminUserRoleRelMapper extends BaseMapper<AdminUserRoleRel> {

    @Select("SELECT COUNT(*) > 0 FROM a_admin_user_role_rel WHERE admin_user_id = #{adminUserId} AND role_id = #{roleId}")
    boolean existsByAdminUserIdAndRoleId(@Param("adminUserId") Long adminUserId, @Param("roleId") Long roleId);
}
