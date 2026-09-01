package com.aichuangzuo.user.modules.generation.controller;

import com.aichuangzuo.user.modules.article.service.ArticleService;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.message.service.MessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 用户端内部回调 controller 的"创作完成/失败推送消息"行为测试。
 *
 * <p>复现 bug：之前 admin worker 完成任务后不会回调 user-api，消息中心永远收不到
 * generation 类的通知。修复后新增的 {@code notifyCompletion} 端点负责把回调翻译成
 * {@link MessageService#pushPersonal} 调用。</p>
 */
@ExtendWith(MockitoExtension.class)
class GenerationTaskInternalControllerNotifyTest {

    @Mock
    private ArticleService articleService;
    @Mock
    private BenefitService benefitService;
    @Mock
    private MessageService messageService;

    @InjectMocks
    private GenerationTaskInternalController controller;

    @Test
    void notifyCompletion_completed_pushesGenerationMessageWithPreviewLink() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", 100);
        payload.put("userId", 200);
        payload.put("status", "completed");
        payload.put("articleBizNo", "ART-100");
        payload.put("articleTitle", "我的标题");

        controller.notifyCompletion(payload);

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(messageService).pushPersonal(
                eq(200L),
                eq("generation"),
                anyString(),
                anyString(),
                linkCaptor.capture(),
                anyString(),
                eq("completed"));
        assertEquals("/console/preview/ART-100", linkCaptor.getValue(),
                "completed 消息的 linkUrl 应指向文章预览页");
    }

    @Test
    void notifyCompletion_failed_pushesGenerationMessageWithFriendlySummary() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", 101);
        payload.put("userId", 201);
        payload.put("status", "failed");
        payload.put("failReason", "AI 调用超时");

        controller.notifyCompletion(payload);

        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
        verify(messageService).pushPersonal(
                eq(201L),
                eq("generation"),
                titleCaptor.capture(),
                summaryCaptor.capture(),
                any(),
                anyString(),
                eq("failed"));
        assertEquals("本次创作失败", titleCaptor.getValue());
        assertEquals("因为未知因素影响，创作失败（本次不消耗次数），请点击重新生成", summaryCaptor.getValue());
    }

    @Test
    void notifyCompletion_summary_containsArticleTitleForCompleted() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", 102);
        payload.put("userId", 202);
        payload.put("status", "completed");
        payload.put("articleBizNo", "ART-102");
        payload.put("articleTitle", "今日爆款标题");

        controller.notifyCompletion(payload);

        ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
        verify(messageService).pushPersonal(
                eq(202L), eq("generation"),
                anyString(), summaryCaptor.capture(), any(), anyString(), eq("completed"));
        assertTrue(summaryCaptor.getValue().contains("今日爆款标题"),
                "completed 摘要应包含文章标题");
    }

    @Test
    void notifyCompletion_failed_titleContainsArticleTitleWhenPresent() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", 103);
        payload.put("userId", 203);
        payload.put("status", "failed");
        payload.put("articleTitle", "我的文章标题");

        controller.notifyCompletion(payload);

        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        verify(messageService).pushPersonal(
                eq(203L), eq("generation"),
                titleCaptor.capture(), anyString(), any(), anyString(), eq("failed"));
        assertEquals("【我的文章标题】本次创作失败", titleCaptor.getValue());
    }

    @Test
    void notifyCompletion_invalidPayload_doesNothing() {
        // 缺 taskId / userId
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "completed");
        payload.put("articleBizNo", "ART-X");
        payload.put("articleTitle", "t");

        controller.notifyCompletion(payload);

        verifyNoInteractions(messageService);
    }

    @Test
    void notifyCompletion_unknownStatus_doesNothing() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", 104);
        payload.put("userId", 204);
        payload.put("status", "aborted");
        payload.put("articleBizNo", "ART-104");

        controller.notifyCompletion(payload);

        verifyNoInteractions(messageService);
    }
}
