package com.aichuangzuo.user.modules.membership.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.membership.dto.request.SubscribeRequest;
import com.aichuangzuo.user.modules.membership.service.MembershipService;
import com.aichuangzuo.user.modules.membership.vo.MembershipStatusVO;
import com.aichuangzuo.user.modules.membership.vo.SubscribeResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端会员订阅接口。
 */
@RestController
@RequestMapping("/api/v1/user/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    /**
     * 立即订阅（测试支付）。
     */
    @PostMapping("/subscribe")
    public Result<SubscribeResultVO> subscribe(@Valid @RequestBody SubscribeRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(membershipService.subscribe(userId, request));
    }

    /**
     * 查询当前用户会员状态。
     */
    @GetMapping("/me")
    public Result<MembershipStatusVO> getMyMembership() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(membershipService.getMyMembership(userId));
    }
}
