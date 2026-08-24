package com.aichuangzuo.user.modules.user.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.user.dto.request.BindInviteCodeRequest;
import com.aichuangzuo.user.modules.user.dto.request.ChangePasswordRequest;
import com.aichuangzuo.user.modules.user.dto.request.UpdateEmailRequest;
import com.aichuangzuo.user.modules.user.dto.request.UpdateNicknameRequest;
import com.aichuangzuo.user.modules.user.dto.request.UpdatePhoneRequest;
import com.aichuangzuo.user.modules.user.service.UserInviteBindingService;
import com.aichuangzuo.user.modules.user.service.UserProfileService;
import com.aichuangzuo.user.modules.user.vo.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户个人资料 REST 接口。
 *
 * <p>路径前缀：/api/v1/user/me，鉴权由 SecurityConfig 的
 * {@code .anyRequest().authenticated()} 统一拦截，
 * 所有方法依赖 JwtAuthenticationFilter 把 userId 写入 SecurityUserContext。
 */
@Tag(name = "用户个人资料")
@RestController
@RequestMapping("/api/v1/user/me")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserInviteBindingService userInviteBindingService;

    /**
     * 获取当前登录用户的个人资料。
     *
     * @return UserProfileVO
     */
    @Operation(summary = "获取我的个人资料")
    @GetMapping
    public Result<UserProfileVO> getMyProfile() {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("获取我的个人资料, userId={}", userId);
        return Result.success(userProfileService.getMyProfile());
    }

    /**
     * 修改昵称。
     *
     * @param request 新昵称（1-20 字符）
     * @return 更新后的 UserProfileVO
     */
    @Operation(summary = "修改昵称")
    @PutMapping("/nickname")
    public Result<UserProfileVO> updateNickname(@Valid @RequestBody UpdateNicknameRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("修改昵称, userId={}, nickname={}", userId, request.getNickname());
        return Result.success(userProfileService.updateNickname(request));
    }

    /**
     * 修改邮箱。需要新邮箱已收到验证码。
     *
     * @param request 新邮箱 + 6 位验证码
     * @return 更新后的 UserProfileVO（email_verified 置 1）
     */
    @Operation(summary = "修改邮箱")
    @PutMapping("/email")
    public Result<UserProfileVO> updateEmail(@Valid @RequestBody UpdateEmailRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("修改邮箱, userId={}, newEmail={}", userId, request.getNewEmail());
        return Result.success(userProfileService.updateEmail(request));
    }

    /**
     * 修改手机号。需要新手机号已收到短信验证码。
     *
     * @param request 新手机号 + 6 位验证码
     * @return 更新后的 UserProfileVO（phone_verified 置 1）
     */
    @Operation(summary = "修改手机号")
    @PutMapping("/phone")
    public Result<UserProfileVO> updatePhone(@Valid @RequestBody UpdatePhoneRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("修改手机号, userId={}, newPhone={}", userId, request.getNewPhone());
        return Result.success(userProfileService.updatePhone(request));
    }

    /**
     * 修改密码。需要原密码校验通过，且账号未被禁用。
     *
     * @param request 旧/新/确认密码
     * @return 成功响应（无 data）
     */
    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("修改密码, userId={}", userId);
        userProfileService.changePassword(request);
        return Result.success();
    }

    /**
     * 绑定邀请人。注册 7 天内且未绑定过邀请人时可补绑。
     *
     * @param request 6 位邀请码
     * @return 成功响应（无 data）
     */
    @Operation(summary = "绑定邀请人")
    @PostMapping("/invite-binding")
    public Result<Void> bindInviteCode(@Valid @RequestBody BindInviteCodeRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("绑定邀请人, userId={}, inviteCode={}", userId, request.getInviteCode());
        userInviteBindingService.bindInviteCode(request);
        return Result.success();
    }

    /**
     * 上传头像。
     *
     * @param file 头像文件（jpg/png，最大 5MB）
     * @return 更新后的 UserProfileVO
     */
    @Operation(summary = "上传头像")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UserProfileVO> updateAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("上传头像, userId={}", userId);
        return Result.success(userProfileService.updateAvatar(file));
    }
}
