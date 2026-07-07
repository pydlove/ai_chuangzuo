package com.aichuangzuo.admin.modules.earnings.controller;

import com.aichuangzuo.admin.modules.earnings.dto.request.AccountQueryRequest;
import com.aichuangzuo.admin.modules.earnings.service.AccountAdminService;
import com.aichuangzuo.admin.modules.earnings.vo.UserAccountDetailVO;
import com.aichuangzuo.admin.modules.earnings.vo.UserAccountPageVO;
import com.aichuangzuo.shared.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端账户明细")
@RestController
@RequestMapping("/api/v1/admin/accounts")
@RequiredArgsConstructor
public class AccountAdminController {

    private final AccountAdminService accountAdminService;

    @Operation(summary = "查询账户列表")
    @GetMapping
    public Result<UserAccountPageVO> list(AccountQueryRequest request) {
        return Result.success(accountAdminService.listAccounts(request));
    }

    @Operation(summary = "查看账户详情")
    @GetMapping("/{userId}")
    public Result<UserAccountDetailVO> detail(@PathVariable Long userId) {
        return Result.success(accountAdminService.getAccountDetail(userId));
    }
}
