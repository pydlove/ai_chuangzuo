package com.aichuangzuo.admin.modules.user.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.auth.service.AdminUserPermissionService;
import com.aichuangzuo.admin.modules.user.dto.request.AdminUserCreateRequest;
import com.aichuangzuo.admin.modules.user.dto.request.AdminUserStatusRequest;
import com.aichuangzuo.admin.modules.user.dto.request.AdminUserUpdateRequest;
import com.aichuangzuo.admin.modules.user.dto.request.ResetCustomSkillQuotaRequest;
import com.aichuangzuo.admin.modules.user.service.AdminUserService;
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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.admin.modules.user.util.UserExcelImportUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "管理端用户管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AdminUserPermissionService adminUserPermissionService;

    @Operation(summary = "查询用户列表")
    @GetMapping
    public Result<AdminUserPageVO> listUsers(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "inviteCode", required = false) String inviteCode,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询用户列表, adminUserId={}, keyword={}, inviteCode={}, page={}, pageSize={}",
                adminUserId, keyword, inviteCode, page, pageSize);
        return Result.success(adminUserService.listUsers(keyword, inviteCode, page, pageSize));
    }

    @Operation(summary = "手动创建用户")
    @PostMapping
    public Result<AdminUserVO> createUser(@Valid @RequestBody AdminUserCreateRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员手动创建用户, adminUserId={}, email={}", adminUserId, request.getEmail());
        return Result.success(adminUserService.createUser(request));
    }

    @Operation(summary = "下载用户导入模板")
    @GetMapping("/import-template")
    public void downloadImportTemplate(HttpServletResponse response) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员下载用户导入模板, adminUserId={}", adminUserId);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String filename = URLEncoder.encode("用户导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"");
        try {
            UserExcelImportUtil.writeTemplate(response.getOutputStream());
        } catch (IOException e) {
            throw new BusinessException(AdminUserErrorCode.EXCEL_PARSE_ERROR);
        }
    }

    @Operation(summary = "从 Excel 批量导入用户")
    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AdminUserImportResultVO> importExcel(@RequestParam("file") MultipartFile file) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员批量导入用户, adminUserId={}, fileName={}, fileSize={}",
                adminUserId, file.getOriginalFilename(), file.getSize());
        return Result.success(adminUserService.importUsersFromExcel(file));
    }

    @Operation(summary = "查看用户详情")
    @GetMapping("/{id}")
    public Result<AdminUserVO> getUser(@PathVariable(name = "id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查看用户详情, adminUserId={}, userId={}", adminUserId, id);
        return Result.success(adminUserService.getUser(id));
    }

    @Operation(summary = "查看用户邀请关系详情")
    @GetMapping("/{id}/invites")
    public Result<AdminUserInviteDetailVO> getUserInvites(@PathVariable(name = "id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查看用户邀请关系详情, adminUserId={}, userId={}", adminUserId, id);
        return Result.success(adminUserService.getUserInviteDetail(id));
    }

    @Operation(summary = "修改用户状态")
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable(name = "id") Long id,
                                     @Valid @RequestBody AdminUserStatusRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员修改用户状态, adminUserId={}, userId={}, status={}",
                adminUserId, id, request.getStatus());
        adminUserService.updateStatus(id, request);
        return Result.success();
    }

    @Operation(summary = "重置用户密码")
    @PostMapping("/{id}/reset-password")
    public Result<AdminUserResetPasswordVO> resetPassword(@PathVariable(name = "id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员重置用户密码, adminUserId={}, userId={}", adminUserId, id);
        return Result.success(adminUserService.resetPassword(id));
    }

    @Operation(summary = "编辑用户全部可改信息")
    @PutMapping("/{id}")
    public Result<AdminUserVO> updateUser(@PathVariable(name = "id") Long id,
                                          @Valid @RequestBody AdminUserUpdateRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员编辑用户, adminUserId={}, userId={}, email={}",
                adminUserId, id, request.getEmail());
        return Result.success(adminUserService.updateUser(id, request));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable(name = "id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员删除用户, adminUserId={}, userId={}", adminUserId, id);
        adminUserService.deleteUser(id);
        return Result.success();
    }

    @Operation(summary = "用户下拉选项（发布者选择）")
    @GetMapping("/options")
    public Result<List<AdminUserOptionVO>> listUserOptions(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询用户下拉选项, adminUserId={}, keyword={}, limit={}",
                adminUserId, keyword, limit);
        return Result.success(adminUserService.listUserOptions(keyword, limit));
    }

    @Operation(summary = "查询用户提示词列表")
    @GetMapping("/{id}/skills")
    public Result<List<AdminUserSkillVO>> listUserSkills(
            @PathVariable(name = "id") Long id,
            @RequestParam(name = "sourceType", defaultValue = "1") Integer sourceType) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询用户提示词列表, adminUserId={}, userId={}, sourceType={}",
                adminUserId, id, sourceType);
        return Result.success(adminUserService.listUserSkills(id, sourceType));
    }

    @Operation(summary = "查询用户已发布/审核中的提示词列表")
    @GetMapping("/{id}/published-skills")
    public Result<List<AdminUserPublishedSkillVO>> listUserPublishedSkills(
            @PathVariable(name = "id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询用户已发布/审核中提示词列表, adminUserId={}, userId={}", adminUserId, id);
        return Result.success(adminUserService.listUserPublishedSkills(id));
    }

    @Operation(summary = "查询用户学习提示词按月统计")
    @GetMapping("/{id}/learned-skills")
    public Result<List<AdminLearnedSkillMonthVO>> listUserLearnedSkillsByMonth(
            @PathVariable(name = "id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询用户学习提示词按月统计, adminUserId={}, userId={}", adminUserId, id);
        return Result.success(adminUserService.listUserLearnedSkillsByMonth(id));
    }

    @Operation(summary = "重置用户指定月份学习提示词额度")
    @PostMapping("/{id}/learned-skills/reset")
    public Result<Void> resetLearnedSkillQuota(
            @PathVariable(name = "id") Long id,
            @RequestParam(name = "period") String period) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员重置用户学习提示词额度, adminUserId={}, userId={}, period={}",
                adminUserId, id, period);
        adminUserService.resetLearnedSkillQuota(id, period);
        return Result.success();
    }

    @Operation(summary = "释放用户自定义提示词额度")
    @PostMapping("/{id}/custom-skill-quota/release")
    public Result<Void> releaseCustomSkillQuota(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody ResetCustomSkillQuotaRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员释放用户自定义提示词额度, adminUserId={}, userId={}, count={}",
                adminUserId, id, request.getCount());
        adminUserService.releaseCustomSkillQuota(id, request);
        return Result.success();
    }

    @Operation(summary = "释放用户提示词市场发布额度")
    @PostMapping("/{id}/published-skills/release-quota")
    public Result<Void> releasePublishSkillQuota(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody ResetCustomSkillQuotaRequest request) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员释放用户提示词市场发布额度, adminUserId={}, userId={}, count={}",
                adminUserId, id, request.getCount());
        adminUserService.releasePublishSkillQuota(id, request);
        return Result.success();
    }

    @Operation(summary = "查询用户收藏的提示词列表")
    @GetMapping("/{id}/favorite-skills")
    public Result<List<AdminUserFavoriteSkillVO>> listUserFavoriteSkills(
            @PathVariable(name = "id") Long id) {
        Long adminUserId = checkSuperAdmin();
        log.info("管理员查询用户收藏提示词列表, adminUserId={}, userId={}", adminUserId, id);
        return Result.success(adminUserService.listUserFavoriteSkills(id));
    }

    private Long checkSuperAdmin() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        if (adminUserId == null || !adminUserPermissionService.isSuperAdmin(adminUserId)) {
            throw new BusinessException(AdminUserErrorCode.NO_PERMISSION);
        }
        return adminUserId;
    }
}
