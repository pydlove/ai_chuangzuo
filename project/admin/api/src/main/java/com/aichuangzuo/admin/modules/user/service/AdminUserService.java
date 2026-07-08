package com.aichuangzuo.admin.modules.user.service;

import com.aichuangzuo.admin.modules.user.dto.request.AdminUserCreateRequest;
import com.aichuangzuo.admin.modules.user.dto.request.AdminUserStatusRequest;
import com.aichuangzuo.admin.modules.user.vo.AdminUserOptionVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserPageVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserResetPasswordVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserVO;

import java.time.LocalDate;
import java.util.List;

public interface AdminUserService {
    AdminUserPageVO listUsers(String keyword, int page, int pageSize);
    AdminUserVO getUser(Long id);
    void updateStatus(Long id, AdminUserStatusRequest request);
    AdminUserResetPasswordVO resetPassword(Long id);
    List<AdminUserOptionVO> listUserOptions(String keyword, int limit);
    AdminUserVO createUser(AdminUserCreateRequest request);
    void updateMembership(Long id, LocalDate expireDate, Long updatedBy);
}
