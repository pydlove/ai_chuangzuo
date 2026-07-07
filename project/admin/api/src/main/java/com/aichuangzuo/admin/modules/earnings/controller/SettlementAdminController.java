package com.aichuangzuo.admin.modules.earnings.controller;

import com.aichuangzuo.admin.modules.earnings.dto.request.SettlementRequest;
import com.aichuangzuo.admin.modules.earnings.service.SettlementAdminService;
import com.aichuangzuo.admin.modules.earnings.vo.PendingSettlementSummaryVO;
import com.aichuangzuo.admin.modules.earnings.vo.PendingSettlementUserVO;
import com.aichuangzuo.admin.modules.earnings.vo.SettlementResultVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理端结算中心")
@RestController
@RequestMapping("/api/v1/admin/accounts/settlements")
@RequiredArgsConstructor
public class SettlementAdminController {

    private final SettlementAdminService settlementAdminService;

    @Operation(summary = "待结算汇总")
    @GetMapping("/pending-summary")
    public Result<PendingSettlementSummaryVO> pendingSummary(@RequestParam(name = "month") String month) {
        return Result.success(settlementAdminService.pendingSummary(month));
    }

    @Operation(summary = "待结算用户列表")
    @GetMapping("/pending-users")
    public Result<List<PendingSettlementUserVO>> pendingUsers(@RequestParam(name = "month") String month) {
        return Result.success(settlementAdminService.pendingUsers(month));
    }

    @Operation(summary = "执行结算")
    @PostMapping("/actions/settle")
    public Result<SettlementResultVO> settle(@Valid @RequestBody SettlementRequest request) {
        return Result.success(settlementAdminService.settle(request));
    }
}
