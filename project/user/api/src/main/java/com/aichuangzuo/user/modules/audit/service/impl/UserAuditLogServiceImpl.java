package com.aichuangzuo.user.modules.audit.service.impl;

import com.aichuangzuo.user.modules.audit.entity.UserAuditLog;
import com.aichuangzuo.user.modules.audit.mapper.UserAuditLogMapper;
import com.aichuangzuo.user.modules.audit.service.UserAuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAuditLogServiceImpl implements UserAuditLogService {

    private final UserAuditLogMapper userAuditLogMapper;

    @Override
    public void save(UserAuditLog log) {
        userAuditLogMapper.insert(log);
    }
}
