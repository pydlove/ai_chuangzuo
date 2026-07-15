package com.aichuangzuo.user.modules.benefit.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
import com.aichuangzuo.user.modules.benefit.vo.UserBenefitVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端会员权益接口。
 */
@RestController
@RequestMapping("/api/v1/user/benefits")
@RequiredArgsConstructor
public class BenefitController {

    private final BenefitService benefitService;

    /**
     * 查询当前用户权益。
     */
    @GetMapping("/me")
    public Result<UserBenefitVO> getMyBenefits() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(benefitService.getMyBenefits(userId));
    }

    /**
     * 校验单项权益。
     */
    @PostMapping("/check/{code}")
    public Result<BenefitCheckVO> check(@PathVariable String code) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(benefitService.check(userId, code));
    }

    /**
     * 消费一次配额（quota 类专用）。
     */
    @PostMapping("/consume/{code}")
    public Result<BenefitCheckVO> consume(@PathVariable String code) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(benefitService.consume(userId, code));
    }
}
