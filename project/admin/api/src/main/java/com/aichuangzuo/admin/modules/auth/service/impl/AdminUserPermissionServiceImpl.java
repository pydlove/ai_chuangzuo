package com.aichuangzuo.admin.modules.auth.service.impl;

import com.aichuangzuo.admin.modules.auth.entity.Role;
import com.aichuangzuo.admin.modules.auth.mapper.AdminUserRoleRelMapper;
import com.aichuangzuo.admin.modules.auth.mapper.RoleMapper;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserPermissionServiceImpl implements AdminUserPermissionService {

    private final RoleMapper roleMapper;
    private final AdminUserRoleRelMapper adminUserRoleRelMapper;

    @Override
    public boolean isSuperAdmin(Long adminUserId) {
        if (adminUserId == null) {
            return false;
        }
        Role role = roleMapper.selectByRoleCode("SUPER_ADMIN");
        if (role == null) {
            return false;
        }
        return adminUserRoleRelMapper.existsByAdminUserIdAndRoleId(adminUserId, role.getId());
    }
}
