package com.aichuangzuo.user.modules.lottery.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.lottery.service.UserCouponService;
import com.aichuangzuo.user.modules.lottery.vo.UserCouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户端-优惠券")
@RestController
@RequestMapping("/api/v1/user/coupons")
@RequiredArgsConstructor
public class UserCouponController {

    private final UserCouponService userCouponService;

    @Operation(summary = "我的优惠券列表")
    @GetMapping
    public Result<List<UserCouponVO>> listMyCoupons() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(userCouponService.listUserCoupons(userId));
    }
}
