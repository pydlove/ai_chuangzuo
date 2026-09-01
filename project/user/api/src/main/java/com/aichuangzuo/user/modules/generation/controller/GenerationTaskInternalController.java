package com.aichuangzuo.user.modules.generation.controller;

import com.aichuangzuo.shared.enums.error.UserGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.result.Result;
import com.aichuangzuo.shared.vo.AiDetectReport;
import com.aichuangzuo.user.modules.article.dto.request.SaveArticleRequest;
import com.aichuangzuo.user.modules.article.service.ArticleService;
import com.aichuangzuo.user.modules.article.vo.ArticleVO;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.message.enums.MessageSubType;
import com.aichuangzuo.user.modules.message.service.MessageService;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.generation.service.GenerationTaskRefundService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    /** 文章生成对应的权益编码。 */
    private static final String ARTICLE_QUOTA_BENEFIT = "ai_article_quota";

    private final ArticleService articleService;
    private final BenefitService benefitService;
    private final MessageService messageService;
    private final GenerationTaskRefundService generationTaskRefundService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * admin worker 调入，保存生成的文章并返回 article.biz_no。
     */
    @PostMapping("/save-article")
    public Result<String> saveArticle(@RequestBody Map<String, Object> payload) {
        Long taskId = asLong(payload.get("taskId"));
        Long userId = asLong(payload.get("userId"));
        String title = asString(payload.get("title"));
        String body = asString(payload.get("body"));

        log.info("内部保存生成文章, userId={}, taskId={}, title={}", SecurityUserContext.getCurrentUserId(), taskId, title);

        if (taskId == null || userId == null || title.isEmpty() || body.isEmpty()) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_INPUT_INVALID);
        }

        SaveArticleRequest req = new SaveArticleRequest();
        req.setTitle(title);
        req.setBody(body);
        req.setPlatform(asString(payload.get("platform")));
        req.setSkill(asString(payload.get("skill")));
        req.setTemplate(asString(payload.get("template")));
        String description = asString(payload.get("description"));
        req.setDescription(description.isEmpty() ? null : description);
        req.setTags(asStringList(payload.get("tags")));
        req.setWordCount(asInt(payload.get("wordCount")));
        req.setAiDetectReport(parseAiDetectReport(payload.get("aiDetectReport")));
        req.setCompletedAt(LocalDateTime.now());
        req.setTaskId(taskId);

        String bizNo = articleService.save(userId, req);
        log.info("task={} user={} article 保存成功 bizNo={}", taskId, userId, bizNo);
        return Result.success(bizNo);
    }

    /**
     * admin 调入，按 articleBizNo 读取已生成文章内容（管理端预览/下载用）。
     */
    @GetMapping("/article/{articleBizNo}")
    public Result<ArticleVO> getArticle(@PathVariable String articleBizNo) {
        log.info("内部查询文章, userId={}, articleBizNo={}", SecurityUserContext.getCurrentUserId(), articleBizNo);
        return Result.success(articleService.getInternal(articleBizNo));
    }

    /**
     * admin worker 调入，退回失败任务预扣的文章额度。
     */
    @PostMapping("/refund-quota")
    public Result<Void> refundQuota(@RequestBody Map<String, Object> payload) {
        Long taskId = asLong(payload.get("taskId"));
        Long userId = asLong(payload.get("userId"));
        log.info("内部退回文章额度, userId={}, taskId={}", SecurityUserContext.getCurrentUserId(), taskId);
        if (taskId == null || userId == null) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_INPUT_INVALID);
        }
        generationTaskRefundService.refundOnce(taskId, userId, ARTICLE_QUOTA_BENEFIT);
        log.info("task={} user={} 退文章额度成功", taskId, userId);
        return Result.success();
    }

    /**
     * admin worker 调入，通知用户任务完成/失败，推送一条 generation 类消息到消息中心。
     *
     * <p>payload 字段：
     * <ul>
     *   <li>taskId        必填</li>
     *   <li>userId        必填</li>
     *   <li>status        必填，取值 {@code completed} / {@code failed}</li>
     *   <li>articleBizNo  completed 时必填，写入 linkUrl（/console/preview/{bizNo}）</li>
     *   <li>articleTitle  completed / failed 时选填，写入摘要/标题</li>
     *   <li>failReason    failed 时选填，仅日志用，不展示给用户</li>
     * </ul>
     *
     * <p>校验失败/状态未知时不抛异常（不阻塞 admin worker），仅记录 warn 日志。
     * 被外部停止（TaskAbortedException）的任务 admin 不会调本接口，因此不重复通知。</p>
     */
    @PostMapping("/notify-completion")
    public Result<Void> notifyCompletion(@RequestBody Map<String, Object> payload) {
        Long taskId = asLong(payload.get("taskId"));
        Long userId = asLong(payload.get("userId"));
        String status = asString(payload.get("status"));
        log.info("内部通知任务完成, userId={}, taskId={}, status={}", SecurityUserContext.getCurrentUserId(), taskId, status);
        if (taskId == null || userId == null || status.isEmpty()) {
            log.warn("notifyCompletion 入参缺失 taskId={} userId={} status={}", taskId, userId, status);
            return Result.success();
        }

        if ("completed".equals(status)) {
            String articleBizNo = asString(payload.get("articleBizNo"));
            if (articleBizNo.isEmpty()) {
                log.warn("notifyCompletion completed 缺 articleBizNo taskId={} userId={}", taskId, userId);
                return Result.success();
            }
            String articleTitle = asString(payload.get("articleTitle"));
            String summary = articleTitle.isEmpty()
                    ? "您的文章已生成完毕，点击查看"
                    : "「" + articleTitle + "」已生成完毕，点击查看";
            String content = articleTitle.isEmpty()
                    ? "您的创作已完成，点击查看详情。"
                    : "您的创作「" + articleTitle + "」已完成，点击查看详情。";
            String linkUrl = "/console/preview/" + articleBizNo;

            messageService.pushPersonal(userId, "generation",
                    "您的文章已生成", summary, linkUrl, content,
                    MessageSubType.GENERATION_COMPLETED.getCode());
            log.info("task={} user={} 推送生成完成消息 articleBizNo={}", taskId, userId, articleBizNo);
        } else if ("failed".equals(status)) {
            String articleTitle = asString(payload.get("articleTitle"));
            String title = articleTitle.isEmpty()
                    ? "本次创作失败"
                    : "【" + articleTitle + "】本次创作失败";
            String summary = "因为未知因素影响，创作失败（本次不消耗次数），请点击重新生成";
            String content = "因为未知因素影响，创作失败（本次不消耗次数），请点击重新生成";

            messageService.pushPersonal(userId, "generation",
                    title, summary, null, content,
                    MessageSubType.GENERATION_FAILED.getCode());
            log.info("task={} user={} 推送生成失败消息 title={}", taskId, userId, title);
        } else {
            log.warn("notifyCompletion 未知 status={} taskId={} userId={}", status, taskId, userId);
        }
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

    private static List<String> asStringList(Object o) {
        if (!(o instanceof List<?> list)) return null;
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item != null && !item.toString().isBlank()) {
                result.add(item.toString());
            }
        }
        return result.isEmpty() ? null : result;
    }

    private AiDetectReport parseAiDetectReport(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(o, AiDetectReport.class);
        } catch (Exception e) {
            log.warn("aiDetectReport 解析失败: {}", e.getMessage());
            return null;
        }
    }
}
