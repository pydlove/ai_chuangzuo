package com.aichuangzuo.user.modules.user.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户端内部接口：供管理端调用，修改指定用户头像。
 * <p>由 {@code InternalKeyAuthenticationFilter} 校验 {@code X-Internal-Key}。
 * 走与用户本人上传一致的存储逻辑，保证各端展示一致。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user/internal/users")
@RequiredArgsConstructor
public class UserInternalController {

    private final UserProfileService userProfileService;

    @PostMapping(value = "/{userId}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Void> updateAvatar(@PathVariable Long userId,
                                     @RequestParam("file") MultipartFile file) {
        log.info("管理端修改用户头像 userId={} size={}", userId, file.getSize());
        userProfileService.updateAvatarForInternal(userId, file);
        return Result.success();
    }
}
