package com.aichuangzuo.admin.modules.settings.wechatofficialaccount.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.settings.wechatofficialaccount.dto.request.WechatOfficialAccountConfigUpdateRequest;
import com.aichuangzuo.admin.modules.settings.wechatofficialaccount.service.WechatOfficialAccountConfigService;
import com.aichuangzuo.admin.modules.settings.wechatofficialaccount.vo.WechatOfficialAccountConfigVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin 端 - 微信公众号配置 API。
 */
@Tag(name = "管理端-微信公众号配置")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/settings/wechat-official-account-config")
@RequiredArgsConstructor
public class WechatOfficialAccountConfigAdminController {

    private final WechatOfficialAccountConfigService service;

    @GetMapping
    public Result<WechatOfficialAccountConfigVO> detail() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询微信公众号配置, adminUserId={}", adminUserId);
        return Result.success(service.detail());
    }

    @PutMapping
    public Result<WechatOfficialAccountConfigVO> update(@Valid @RequestBody WechatOfficialAccountConfigUpdateRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员更新微信公众号配置, adminUserId={}", adminUserId);
        return Result.success(service.update(request, adminUserId));
    }

    @PostMapping("/publish-menu")
    public Result<Void> publishMenu() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员发布微信公众号菜单, adminUserId={}", adminUserId);
        service.publishMenu();
        return Result.success();
    }
}
