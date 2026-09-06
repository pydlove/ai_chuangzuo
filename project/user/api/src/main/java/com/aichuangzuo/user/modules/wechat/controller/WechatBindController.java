package com.aichuangzuo.user.modules.wechat.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.wechat.service.WechatBindService;
import com.aichuangzuo.user.modules.wechat.vo.WechatBindCodeVO;
import com.aichuangzuo.user.modules.wechat.vo.WechatBindQrVO;
import com.aichuangzuo.user.modules.wechat.vo.WechatBindStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端微信公众号绑定接口。
 */
@Tag(name = "用户端-公众号绑定")
@RestController
@RequestMapping("/api/v1/user/wechat")
@RequiredArgsConstructor
@Slf4j
public class WechatBindController {

    private final WechatBindService wechatBindService;

    @Operation(summary = "生成公众号绑定二维码")
    @GetMapping("/bind/qr-code")
    public Result<WechatBindQrVO> generateQrCode() {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("生成公众号绑定二维码, userId={}", userId);
        return Result.success(wechatBindService.generateBindQrCode(userId));
    }

    @Operation(summary = "生成公众号绑定码")
    @PostMapping("/bind/code")
    public Result<WechatBindCodeVO> generateBindCode() {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("生成公众号绑定码, userId={}", userId);
        return Result.success(wechatBindService.generateBindCode(userId));
    }

    @Operation(summary = "查询当前用户绑定状态")
    @GetMapping("/bind/status")
    public Result<WechatBindStatusVO> checkStatus() {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("查询公众号绑定状态, userId={}", userId);
        return Result.success(wechatBindService.checkBindStatus(userId));
    }

    @Operation(summary = "根据场景值查询绑定状态")
    @GetMapping("/bind/status-by-scene")
    public Result<WechatBindStatusVO> checkStatusByScene(@RequestParam("sceneStr") String sceneStr) {
        log.info("根据场景值查询绑定状态, sceneStr={}", sceneStr);
        return Result.success(wechatBindService.checkBindStatusBySceneStr(sceneStr));
    }
}
