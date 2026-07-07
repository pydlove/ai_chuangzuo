package com.aichuangzuo.admin.modules.earnings.controller;

import com.aichuangzuo.admin.modules.earnings.dto.request.LeaderboardGrantRequest;
import com.aichuangzuo.admin.modules.earnings.service.LeaderboardAwardService;
import com.aichuangzuo.admin.modules.earnings.vo.LeaderboardTop10VO;
import com.aichuangzuo.admin.modules.earnings.vo.RewardRecordAdminVO;
import com.aichuangzuo.shared.result.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理端榜单发奖")
@RestController
@RequestMapping("/api/v1/admin/leaderboards/rewards")
@RequiredArgsConstructor
public class LeaderboardAwardController {

    private final LeaderboardAwardService leaderboardAwardService;

    @Operation(summary = "榜单 TOP 10 预览")
    @GetMapping("/preview")
    public Result<List<LeaderboardTop10VO>> preview(
            @RequestParam(name = "leaderboardType") Integer leaderboardType,
            @RequestParam(name = "periodMonth") String periodMonth) {
        return Result.success(leaderboardAwardService.preview(leaderboardType, periodMonth));
    }

    @Operation(summary = "执行发奖")
    @PostMapping("/actions/grant")
    public Result<Map<String, Integer>> grant(@Valid @RequestBody LeaderboardGrantRequest request) {
        int granted = leaderboardAwardService.grant(request);
        return Result.success(Map.of("granted", granted));
    }

    @Operation(summary = "奖励历史")
    @GetMapping
    public Result<Page<RewardRecordAdminVO>> list(
            @RequestParam(name = "leaderboardType", required = false) Integer leaderboardType,
            @RequestParam(name = "periodMonth", required = false) String periodMonth,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return Result.success(leaderboardAwardService.listRewards(leaderboardType, periodMonth, page, size));
    }
}
