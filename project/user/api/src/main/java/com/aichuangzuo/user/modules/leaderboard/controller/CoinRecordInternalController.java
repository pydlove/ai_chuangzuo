package com.aichuangzuo.user.modules.leaderboard.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.leaderboard.dto.request.InternalGrantRequest;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user/coin-records")
@RequiredArgsConstructor
public class CoinRecordInternalController {

    private final CoinRecordService coinRecordService;

    @PostMapping("/internal-grant")
    public Result<Map<String, String>> internalGrant(@Valid @RequestBody InternalGrantRequest request) {
        String bizNo = coinRecordService.grant(
                request.getUserId(),
                request.getBizType(),
                request.getAmount(),
                request.getRefId(),
                request.getRemark());
        return Result.success(Map.of("coinRecordBizNo", bizNo));
    }
}
