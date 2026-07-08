package com.aichuangzuo.user.modules.earnings.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.earnings.dto.request.ListEarningsRequest;
import com.aichuangzuo.user.modules.earnings.service.EarningsService;
import com.aichuangzuo.user.modules.earnings.vo.AccountSummaryVO;
import com.aichuangzuo.user.modules.earnings.vo.EarningsRecordPageVO;
import com.aichuangzuo.user.modules.earnings.vo.MonthlySettlementVO;
import com.aichuangzuo.user.modules.earnings.vo.SettleLastMonthResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户账户与收益接口。
 */
@Tag(name = "用户账户收益")
@RestController
@RequestMapping("/api/v1/user/account")
@RequiredArgsConstructor
public class AccountController {

    private final EarningsService earningsService;

    @Operation(summary = "账户收益汇总")
    @GetMapping("/summary")
    public Result<AccountSummaryVO> summary() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(earningsService.getSummary(userId));
    }

    @Operation(summary = "按月结算列表")
    @GetMapping("/settlement-list")
    public Result<List<MonthlySettlementVO>> settlementList() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(earningsService.getMonthlySettlementList(userId));
    }

    @Operation(summary = "收益记录分页")
    @GetMapping("/earnings")
    public Result<EarningsRecordPageVO> earnings(@Valid ListEarningsRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(earningsService.listEarnings(userId, request));
    }

    @Operation(summary = "结算上月收益")
    @PostMapping("/settle-last-month")
    public Result<SettleLastMonthResultVO> settleLastMonth() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(earningsService.settleLastMonth(userId));
    }
}
