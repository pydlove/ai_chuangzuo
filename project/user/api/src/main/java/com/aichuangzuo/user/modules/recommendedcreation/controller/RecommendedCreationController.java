package com.aichuangzuo.user.modules.recommendedcreation.controller;

import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.generation.vo.GenerationTaskVO;
import com.aichuangzuo.user.modules.recommendedcreation.dto.request.GenerateAnglesRequest;
import com.aichuangzuo.user.modules.recommendedcreation.dto.request.UpdateSessionRequest;
import com.aichuangzuo.user.modules.recommendedcreation.service.RecommendedCreationService;
import com.aichuangzuo.user.modules.recommendedcreation.vo.AngleOptionVO;
import com.aichuangzuo.user.modules.recommendedcreation.vo.RecommendedCreationSessionVO;
import com.aichuangzuo.user.modules.recommendedcreation.vo.TopicOptionVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "用户端-小爱推荐创作")
@RestController
@RequestMapping("/api/v1/user/recommended-creation")
@RequiredArgsConstructor
public class RecommendedCreationController {

    private final RecommendedCreationService recommendedCreationService;

    @GetMapping("/session")
    public Result<RecommendedCreationSessionVO> getSession() {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("查询小爱推荐创作会话, userId={}", userId);
        return Result.success(recommendedCreationService.getSession(userId));
    }

    @PostMapping("/topics")
    public Result<List<TopicOptionVO>> generateTopics() {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("生成小爱推荐选题, userId={}", userId);
        return Result.success(recommendedCreationService.generateTopics(userId));
    }

    @PostMapping("/angles")
    public Result<List<AngleOptionVO>> generateAngles(@Valid @RequestBody GenerateAnglesRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("生成小爱推荐观点, userId={}, topicId={}", userId, request.getTopicId());
        return Result.success(recommendedCreationService.generateAngles(userId, request.getTopicId()));
    }

    @PatchMapping("/session")
    public Result<Void> updateSession(@Valid @RequestBody UpdateSessionRequest request) {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("更新小爱推荐创作会话, userId={}, currentStep={}", userId, request.getCurrentStep());
        recommendedCreationService.updateSession(userId, request);
        return Result.success();
    }

    @PostMapping("/submit")
    public Result<GenerationTaskVO> submitGeneration() {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("提交小爱推荐创作任务, userId={}", userId);
        return Result.success(recommendedCreationService.submitGeneration(userId));
    }

    @DeleteMapping("/session")
    public Result<Void> clearSession() {
        Long userId = SecurityUserContext.getCurrentUserId();
        log.info("清除小爱推荐创作会话, userId={}", userId);
        recommendedCreationService.clearSession(userId);
        return Result.success();
    }
}
