package com.aichuangzuo.user.modules.audit.service;

import com.aichuangzuo.user.modules.audit.entity.UserAuditLog;

public interface UserAuditLogService {

    void save(UserAuditLog log);
}
