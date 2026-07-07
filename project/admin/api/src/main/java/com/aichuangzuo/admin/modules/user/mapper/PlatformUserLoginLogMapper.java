package com.aichuangzuo.admin.modules.user.mapper;

import com.aichuangzuo.admin.modules.user.entity.PlatformUserLoginLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface PlatformUserLoginLogMapper extends BaseMapper<PlatformUserLoginLog> {

    @Select("SELECT created_at FROM u_user_login_log WHERE user_id = #{userId} AND login_status = 1 ORDER BY created_at DESC LIMIT 1")
    LocalDateTime selectLastLoginAtByUserId(@Param("userId") Long userId);
}
