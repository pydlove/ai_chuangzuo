package com.aichuangzuo.user.modules.auth.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.auth.service.CaptchaService;
import com.aichuangzuo.user.modules.auth.vo.CaptchaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户认证")
@RestController
@RequestMapping("/api/v1/user/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CaptchaService captchaService;

    @Operation(summary = "获取图形验证码")
    @GetMapping("/captcha")
    public Result<CaptchaVO> getCaptcha() {
        return Result.success(captchaService.generateCaptcha());
    }
}
