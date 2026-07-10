package com.aichuangzuo.user.modules.generation.controller;

import com.aichuangzuo.shared.enums.error.UserGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.user.modules.article.dto.request.SaveArticleRequest;
import com.aichuangzuo.user.modules.article.service.ArticleService;
import com.aichuangzuo.user.modules.leaderboard.service.CoinRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户端内部接口：供管理端 worker 调用。
 * <p>由 {@code InternalKeyAuthenticationFilter} 校验 {@code X-Internal-Key}。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user/internal/generation")
@RequiredArgsConstructor
public class GenerationTaskInternalController {

    /** 单次生成扣 1 个创作币。 */
    private static final BigDecimal GENERATION_QUOTA = BigDecimal.ONE;

    private final ArticleService articleService;
    private final CoinRecordService coinRecordService;

    /**
     * admin worker 调入，保存生成的文章并返回 article.biz_no。
     */
    @PostMapping("/save-article")
    public Result<String> saveArticle(@RequestBody Map<String, Object> payload) {
        Long taskId = asLong(payload.get("taskId"));
        Long userId = asLong(payload.get("userId"));
        String title = asString(payload.get("title"));
        String body = asString(payload.get("body"));

        if (taskId == null || userId == null || title.isEmpty() || body.isEmpty()) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_INPUT_INVALID);
        }

        SaveArticleRequest req = new SaveArticleRequest();
        req.setTitle(title);
        req.setBody(body);
        req.setPlatform(asString(payload.get("platform")));
        req.setStyle(asString(payload.get("style")));
        req.setWordCount(asInt(payload.get("wordCount")));
        req.setCompletedAt(LocalDateTime.now());

        String bizNo = articleService.save(userId, req);
        log.info("task={} user={} article 保存成功 bizNo={}", taskId, userId, bizNo);
        return Result.success(bizNo);
    }

    /**
     * admin worker 调入，退回失败任务预扣的创作币。
     */
    @PostMapping("/refund-quota")
    public Result<Void> refundQuota(@RequestBody Map<String, Object> payload) {
        Long taskId = asLong(payload.get("taskId"));
        Long userId = asLong(payload.get("userId"));
        if (taskId == null || userId == null) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_INPUT_INVALID);
        }
        String refId = "TASK" + taskId;
        String bizNo = coinRecordService.grant(userId, "generation_refund",
                GENERATION_QUOTA, refId, "创作生成失败退还");
        log.info("task={} user={} 退额度成功 bizNo={}", taskId, userId, bizNo);
        return Result.success();
    }

    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }

    private static Integer asInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return null; }
    }

    private static String asString(Object o) {
        if (o == null) return "";
        return o.toString();
    }
}
