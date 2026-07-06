package com.aichuangzuo.user.modules.user.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.user.dto.request.ChangePasswordRequest;
import com.aichuangzuo.user.modules.user.dto.request.UpdateEmailRequest;
import com.aichuangzuo.user.modules.user.dto.request.UpdateNicknameRequest;
import com.aichuangzuo.user.modules.user.service.UserProfileService;
import com.aichuangzuo.user.modules.user.vo.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * 获取当前登录用户的个人资料。
     *
     * @return UserProfileVO
     */
    @Operation(summary = "获取我的个人资料")
    @GetMapping
    public Result<UserProfileVO> getMyProfile() {
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
        return Result.success(userProfileService.updateEmail(request));
    }

    /**
     * 修改密码。需要原密码校验通过。
     *
     * @param request 旧/新/确认密码
     * @return 成功响应（无 data）
     */
    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userProfileService.changePassword(request);
        return Result.success();
    }
}
