package com.aichuangzuo.user.modules.membership.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.membership.service.PaymentService;
import com.aichuangzuo.user.modules.membership.vo.PaymentConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端支付配置接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 获取当前支付模式配置（公开）。
     */
    @GetMapping("/config")
    public Result<PaymentConfigVO> getConfig() {
        return Result.success(paymentService.getPaymentConfig());
    }
}
