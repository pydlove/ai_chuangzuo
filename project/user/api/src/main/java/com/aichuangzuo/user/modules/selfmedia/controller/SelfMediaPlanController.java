package com.aichuangzuo.user.modules.selfmedia.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.selfmedia.dto.request.*;
import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanService;
import com.aichuangzuo.user.modules.selfmedia.vo.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "用户端-自媒体方案")
@RestController
@RequestMapping("/api/v1/user/self-media-plans")
@RequiredArgsConstructor
public class SelfMediaPlanController {

    private final SelfMediaPlanService planService;

    @GetMapping("/current")
    public Result<SelfMediaPlanVO> getCurrentPlan() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(planService.getCurrentPlan(userId));
    }

    @GetMapping("/platform-questions")
    public Result<List<QuestionVO>> getPlatformQuestions(@RequestParam("platformKey") String platformKey) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(planService.getOrGeneratePlatformQuestions(userId, platformKey));
    }

    @PostMapping("/actions/recommend-niches")
    public Result<List<NicheOptionVO>> recommendNiches(@RequestBody RecommendNichesRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(planService.recommendNiches(userId, request));
    }

    @PostMapping("/actions/recommend-personas")
    public Result<RecommendPersonasResultVO> recommendPersonas(@RequestBody RecommendPersonasRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(planService.recommendPersonas(userId, request));
    }

    @PostMapping
    public Result<SelfMediaPlanVO> savePlan(@RequestBody SavePlanRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(planService.savePlan(userId, request));
    }
}
