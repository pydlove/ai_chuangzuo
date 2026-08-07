package com.aichuangzuo.user.modules.audit.mapper;

import com.aichuangzuo.user.modules.audit.entity.UserAuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserAuditLogMapper extends BaseMapper<UserAuditLog> {
}
