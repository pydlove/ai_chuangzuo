package com.aichuangzuo.admin.modules.user.service;

import com.aichuangzuo.admin.modules.user.dto.request.AdminUserCreateRequest;
import com.aichuangzuo.admin.modules.user.dto.request.AdminUserStatusRequest;
import com.aichuangzuo.admin.modules.user.dto.request.AdminUserUpdateRequest;
import com.aichuangzuo.admin.modules.user.dto.request.ResetCustomSkillQuotaRequest;
import com.aichuangzuo.admin.modules.user.vo.AdminLearnedSkillMonthVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserFavoriteSkillVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserImportResultVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserInviteDetailVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserOptionVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserPageVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserPublishedSkillVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserResetPasswordVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserSkillVO;
import com.aichuangzuo.admin.modules.user.vo.AdminUserVO;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminUserService {
    AdminUserPageVO listUsers(String keyword, String inviteCode, int page, int pageSize);
    AdminUserVO getUser(Long id);
    AdminUserInviteDetailVO getUserInviteDetail(Long id, int page, int pageSize);
    void updateStatus(Long id, AdminUserStatusRequest request);
    AdminUserResetPasswordVO resetPassword(Long id);
    List<AdminUserOptionVO> listUserOptions(String keyword, int limit);
    AdminUserVO createUser(AdminUserCreateRequest request);
    AdminUserVO updateUser(Long id, AdminUserUpdateRequest request);
    void deleteUser(Long id);

    /**
     * 批量删除用户。
     *
     * @param ids 用户 ID 列表
     * @return 实际删除数量
     */
    int deleteBatch(List<Long> ids);

    /**
     * 从 Excel 批量导入用户。
     *
     * @param file Excel 文件
     * @return 导入结果
     */
    AdminUserImportResultVO importUsersFromExcel(MultipartFile file);

    /**
     * 查询用户提示词列表。
     *
     * @param userId 用户ID
     * @param sourceType 来源类型：1-自定义，2-学习
     * @return 提示词列表
     */
    List<AdminUserSkillVO> listUserSkills(Long userId, Integer sourceType);

    /**
     * 查询用户学习提示词按月统计。
     *
     * @param userId 用户ID
     * @return 按月统计列表
     */
    List<AdminLearnedSkillMonthVO> listUserLearnedSkillsByMonth(Long userId);

    /**
     * 重置用户当月学习提示词额度。
     *
     * @param userId 用户ID
     * @param period 周期 yyyy-MM
     */
    void resetLearnedSkillQuota(Long userId, String period);

    /**
     * 释放用户自定义提示词额度指定数量。
     *
     * @param userId 用户ID
     * @param request 释放数量
     */
    void releaseCustomSkillQuota(Long userId, ResetCustomSkillQuotaRequest request);

    /**
     * 释放用户提示词市场发布额度指定数量。
     *
     * @param userId 用户ID
     * @param request 释放数量
     */
    void releasePublishSkillQuota(Long userId, ResetCustomSkillQuotaRequest request);

    /**
     * 查询用户已发布/审核中的提示词列表（来自提示词市场）。
     *
     * @param userId 用户ID
     * @return 已发布提示词列表
     */
    List<AdminUserPublishedSkillVO> listUserPublishedSkills(Long userId);

    /**
     * 查询用户收藏的提示词列表（来自提示词市场）。
     *
     * @param userId 用户ID
     * @return 收藏提示词列表
     */
    List<AdminUserFavoriteSkillVO> listUserFavoriteSkills(Long userId);
}
