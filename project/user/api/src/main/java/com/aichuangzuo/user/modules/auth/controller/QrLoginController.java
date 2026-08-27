package com.aichuangzuo.user.modules.auth.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.auth.dto.request.QrLoginAuthorizeRequest;
import com.aichuangzuo.user.modules.auth.dto.request.QrLoginScanRequest;
import com.aichuangzuo.user.modules.auth.service.QrLoginService;
import com.aichuangzuo.user.modules.auth.vo.AuthTokenVO;
import com.aichuangzuo.user.modules.auth.vo.QrLoginCreateVO;
import com.aichuangzuo.user.modules.auth.vo.QrLoginStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "二维码登录")
@RestController
@RequestMapping("/api/v1/user/auth/qr-login")
@RequiredArgsConstructor
@Slf4j
public class QrLoginController {

    private final QrLoginService qrLoginService;

    @Operation(summary = "创建二维码登录会话")
    @PostMapping("/create")
    public Result<QrLoginCreateVO> create(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        log.info("创建二维码登录会话, clientIp={}", clientIp);
        return Result.success(qrLoginService.create(clientIp, userAgent));
    }

    @Operation(summary = "查询二维码登录会话状态")
    @GetMapping("/status")
    public Result<QrLoginStatusVO> status(@RequestParam("qrCode") String qrCode) {
        return Result.success(qrLoginService.getStatus(qrCode));
    }

    @Operation(summary = "手机端扫描二维码")
    @PostMapping("/scan")
    public Result<QrLoginStatusVO> scan(@Valid @RequestBody QrLoginScanRequest request) {
        Long scannerUserId = SecurityUserContext.getCurrentUserId();
        log.info("二维码登录扫描, qrCode={}, scannerUserId={}", request.getQrCode(), scannerUserId);
        return Result.success(qrLoginService.scan(request, scannerUserId));
    }

    @Operation(summary = "桌面端授权二维码登录")
    @PostMapping("/authorize")
    public Result<AuthTokenVO> authorize(@Valid @RequestBody QrLoginAuthorizeRequest request,
                                          HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return Result.success(qrLoginService.authorize(request, clientIp, userAgent));
    }

    @Operation(summary = "取消二维码登录会话")
    @PostMapping("/cancel")
    public Result<Void> cancel(@RequestParam("qrCode") String qrCode) {
        qrLoginService.cancel(qrCode);
        return Result.success();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}
