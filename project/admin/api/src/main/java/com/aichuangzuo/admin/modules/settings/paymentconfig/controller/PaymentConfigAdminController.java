package com.aichuangzuo.admin.modules.settings.paymentconfig.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.settings.paymentconfig.dto.request.PaymentConfigUpdateRequest;
import com.aichuangzuo.admin.modules.settings.paymentconfig.service.PaymentConfigService;
import com.aichuangzuo.admin.modules.settings.paymentconfig.vo.PaymentConfigVO;
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
 * Admin 端 - 支付配置 API。
 */
@Tag(name = "管理端-支付配置")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/settings/payment-config")
@RequiredArgsConstructor
public class PaymentConfigAdminController {

    private final PaymentConfigService service;

    @GetMapping
    public Result<PaymentConfigVO> detail() {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员查询支付配置, adminUserId={}", adminUserId);
        return Result.success(service.detail());
    }

    @PutMapping
    public Result<PaymentConfigVO> update(@Valid @RequestBody PaymentConfigUpdateRequest request) {
        Long adminUserId = SecurityAdminContext.getCurrentAdminUserId();
        log.info("管理员更新支付配置, adminUserId={}, testMode={}",
                adminUserId, request.getTestMode());
        return Result.success(service.update(request, adminUserId));
    }
}
