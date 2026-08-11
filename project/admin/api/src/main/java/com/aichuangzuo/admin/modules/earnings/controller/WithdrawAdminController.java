package com.aichuangzuo.admin.modules.earnings.controller;

import com.aichuangzuo.admin.infrastructure.security.SecurityAdminContext;
import com.aichuangzuo.admin.modules.earnings.dto.request.WithdrawQueryRequest;
import com.aichuangzuo.admin.modules.earnings.dto.request.WithdrawRejectRequest;
import com.aichuangzuo.admin.modules.earnings.service.WithdrawAdminService;
import com.aichuangzuo.admin.modules.earnings.vo.WithdrawAdminPageVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端提现管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/withdrawals")
@RequiredArgsConstructor
public class WithdrawAdminController {

    private final WithdrawAdminService withdrawAdminService;

    @Operation(summary = "查询提现申请列表")
    @GetMapping
    public Result<WithdrawAdminPageVO> list(WithdrawQueryRequest request) {
        Long adminUserId = currentAdminId();
        log.info("管理员查询提现申请列表, adminUserId={}, userId={}, bizNo={}, status={}, page={}, size={}",
                adminUserId, request.getUserId(), request.getBizNo(), request.getStatus(),
                request.getPage(), request.getSize());
        return Result.success(withdrawAdminService.listWithdrawRequests(request));
    }

    @Operation(summary = "通过提现申请")
    @PostMapping("/{bizNo}/approve")
    public Result<Void> approve(@PathVariable("bizNo") String bizNo) {
        Long adminUserId = currentAdminId();
        log.info("管理员通过提现申请, adminUserId={}, bizNo={}", adminUserId, bizNo);
        withdrawAdminService.approve(bizNo, adminUserId);
        return Result.success();
    }

    @Operation(summary = "拒绝提现申请")
    @PostMapping("/{bizNo}/reject")
    public Result<Void> reject(@PathVariable("bizNo") String bizNo,
                                @Valid @RequestBody WithdrawRejectRequest request) {
        Long adminUserId = currentAdminId();
        log.info("管理员拒绝提现申请, adminUserId={}, bizNo={}, remark={}",
                adminUserId, bizNo, request.getRemark());
        withdrawAdminService.reject(bizNo, adminUserId, request.getRemark());
        return Result.success();
    }

    private Long currentAdminId() {
        Long adminId = SecurityAdminContext.getCurrentAdminUserId();
        return adminId != null ? adminId : 0L;
    }
}
