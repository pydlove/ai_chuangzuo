package com.aichuangzuo.admin.modules.user.service;

import com.aichuangzuo.admin.modules.user.dto.request.AdminUserStatusRequest;
import com.aichuangzuo.admin.modules.user.vo.AdminUserPageVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserResetPasswordVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserVO;

public interface AdminUserService {
    AdminUserPageVO listUsers(String keyword, int page, int pageSize);
    AdminUserVO getUser(Long id);
    void updateStatus(Long id, AdminUserStatusRequest request);
    AdminUserResetPasswordVO resetPassword(Long id);
}
