package com.aichuangzuo.user.modules.selfmedia.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
import com.aichuangzuo.user.modules.selfmedia.dto.request.*;
import com.aichuangzuo.user.modules.selfmedia.service.PublishPlanAiService;
import com.aichuangzuo.user.modules.selfmedia.service.SelfMediaPlanService;
import com.aichuangzuo.user.modules.selfmedia.vo.*;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.enums.error.BenefitErrorCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    private static final String BENEFIT_REPOST_PLAN = "repost_plan";

    private final SelfMediaPlanService planService;
    private final PublishPlanAiService publishPlanAiService;
    private final BenefitService benefitService;

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

    @GetMapping("/publish-plan")
    public Result<PublishPlanGuideVO> getPublishPlan(@RequestParam("mainPlatform") String mainPlatform) {
        Long userId = SecurityUserContext.getCurrentUserId();
        BenefitCheckVO check = benefitService.check(userId, BENEFIT_REPOST_PLAN);
        if (!Boolean.TRUE.equals(check.getAllowed())) {
            throw new BusinessException(BenefitErrorCode.BENEFIT_NOT_SUPPORTED.getCode(), check.getMessage());
        }
        return Result.success(publishPlanAiService.getCachedPlan(userId, mainPlatform));
    }

    @PostMapping("/actions/publish-plan")
    public Result<PublishPlanGuideVO> generatePublishPlan(@Valid @RequestBody PublishPlanRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        BenefitCheckVO check = benefitService.check(userId, BENEFIT_REPOST_PLAN);
        if (!Boolean.TRUE.equals(check.getAllowed())) {
            throw new BusinessException(BenefitErrorCode.BENEFIT_NOT_SUPPORTED.getCode(), check.getMessage());
        }
        return Result.success(publishPlanAiService.generatePlan(userId, request.getMainPlatform()));
    }
}
