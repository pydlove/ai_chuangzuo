
package com.aichuangzuo.admin.modules.security.smsconfig.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.security.smsconfig.dto.request.SmsConfigUpdateRequest;
import com.aichuangzuo.admin.modules.security.smsconfig.service.SmsConfigService;
import com.aichuangzuo.admin.modules.security.smsconfig.vo.SmsConfigVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin 端 - 短信配置 API。
 */
@Tag(name = "管理端-短信配置")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/security/sms-config")
@RequiredArgsConstructor
public class SmsConfigAdminController {

    private final SmsConfigService service;

    @GetMapping
    public Result<SmsConfigVO> detail() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询短信配置, adminUserId={}", adminUserId);
        return Result.success(service.detail());
    }

    @PutMapping
    public Result<SmsConfigVO> update(@Valid @RequestBody SmsConfigUpdateRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员更新短信配置, adminUserId={}, enabled={}", adminUserId, request.getEnabled());
        return Result.success(service.update(request, adminUserId));
    }
}
