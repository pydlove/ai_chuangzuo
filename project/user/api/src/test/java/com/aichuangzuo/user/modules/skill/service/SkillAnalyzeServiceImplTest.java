package com.aichuangzuo.user.modules.skill.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
import com.aichuangzuo.user.modules.skill.analyze.config.service.SkillAnalyzeConfigService;
import com.aichuangzuo.user.modules.skill.analyze.service.SkillAnalyzeDailyLimiter;
import com.aichuangzuo.user.modules.skill.service.impl.SkillAnalyzeServiceImpl;
import com.aichuangzuo.user.modules.skill.vo.SkillAnalyzeVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SkillAnalyzeServiceImpl 纯单测：mock AI 调用器 + mock 配置/限流器，不起 Spring 上下文。
 */
class SkillAnalyzeServiceImplTest {

    private static final long USER_ID = 1L;

    private static final String ARTICLE = """
            清晨的巷子总是被豆浆的香气唤醒，老人们坐在门口闲聊。

            我沿着青石板路慢慢走，看阳光一点点爬上斑驳的墙。卖花的阿婆已经摆好了摊子，茉莉的清香混着露水的味道。

            这样的日子很慢，慢到可以听见自己的心跳。城市的喧嚣在这里被按下了静音键，剩下的只有生活本来的模样。
            """;

    private static final String VALID_PROMPT =
            "你是一位中文写手，请模仿以下参考文章的写作风格：\n\n"
                    + "【语气】温和怀旧，与读者平等对话\n"
                    + "【词汇】书面化，不用网络用语\n"
                    + "【句式】短句为主，节奏舒缓\n"
                    + "【结构】以场景开头，结尾抒情收束\n\n"
                    + "请在生成新内容时严格遵循以上风格特征。";

    private static final String VALID_JSON = """
            {"excerpt1":"清晨的巷子总是被豆浆的香气唤醒，老人们坐在门口闲聊。","excerpt2":"慢到可以听见自己的心跳","description":"怀旧温和的生活散文风格，适合描写慢节奏日常与细腻情感","prompt":"%s"}
            """.formatted(VALID_PROMPT.replace("\n", "\\n").replace("\"", "\\\""));

    private SkillAnalyzeConfigService mockConfigService() {
        SkillAnalyzeConfigService cs = mock(SkillAnalyzeConfigService.class);
        when(cs.getDailyAttemptLimit()).thenReturn(5);
        return cs;
    }

    private SkillAnalyzeDailyLimiter mockDailyLimiter() {
        SkillAnalyzeDailyLimiter limiter = mock(SkillAnalyzeDailyLimiter.class);
        return limiter;
    }

    private BenefitService mockBenefitService() {
        BenefitService benefitService = mock(BenefitService.class);
        BenefitCheckVO vo = new BenefitCheckVO();
        vo.setAllowed(true);
        vo.setCode("skill_learn_analyze");
        vo.setType("boolean");
        vo.setValue("true");
        when(benefitService.check(anyLong(), anyString())).thenReturn(vo);
        return benefitService;
    }

    private SkillAnalyzeServiceImpl serviceWith(String aiResponse) {
        SkillAnalyzeAiService aiService = mock(SkillAnalyzeAiService.class);
        when(aiService.call(anyString(), anyString())).thenReturn(aiResponse);
        return new SkillAnalyzeServiceImpl(aiService, mockConfigService(), mockDailyLimiter(), mockBenefitService(), new ObjectMapper());
    }

    @Test
    void analyze_shouldReturnParsedResultOnCleanJson() {
        SkillAnalyzeVO vo = serviceWith(VALID_JSON).analyze(USER_ID, ARTICLE);

        assertEquals("清晨的巷子总是被豆浆的香气唤醒，老人们坐在门口闲聊。", vo.getExcerpt1());
        assertEquals("慢到可以听见自己的心跳", vo.getExcerpt2());
        assertEquals(VALID_PROMPT, vo.getPrompt());
        assertEquals("怀旧温和的生活散文风格，适合描写慢节奏日常与细腻情感", vo.getDescription());
    }

    @Test
    void analyze_shouldStripCodeFence() {
        SkillAnalyzeVO vo = serviceWith("```json\n" + VALID_JSON + "\n```").analyze(USER_ID, ARTICLE);

        assertEquals(VALID_PROMPT, vo.getPrompt());
    }

    @Test
    void analyze_shouldExtractJsonFromSurroundingText() {
        SkillAnalyzeVO vo = serviceWith("好的，以下是分析结果：\n" + VALID_JSON + "\n希望对你有帮助。").analyze(USER_ID, ARTICLE);

        assertEquals(VALID_PROMPT, vo.getPrompt());
        assertEquals("清晨的巷子总是被豆浆的香气唤醒，老人们坐在门口闲聊。", vo.getExcerpt1());
    }

    @Test
    void analyze_shouldThrowOnInvalidJson() {
        assertThrows(BusinessException.class, () -> serviceWith("这不是 JSON").analyze(USER_ID, ARTICLE));
    }

    @Test
    void analyze_shouldThrowWhenPromptMissingMarker() {
        String badPrompt = "你是一位中文写手。【语气】温和【词汇】书面【句式】短句为主，没有结构标记";
        String json = """
                {"excerpt1":"","excerpt2":"","prompt":"%s"}
                """.formatted(badPrompt);
        assertThrows(BusinessException.class, () -> serviceWith(json).analyze(USER_ID, ARTICLE));
    }

    @Test
    void analyze_shouldThrowWhenPromptTooLong() {
        String longPrompt = VALID_PROMPT + "长".repeat(1200);
        String json = """
                {"excerpt1":"","excerpt2":"","prompt":"%s"}
                """.formatted(longPrompt.replace("\n", "\\n"));
        assertThrows(BusinessException.class, () -> serviceWith(json).analyze(USER_ID, ARTICLE));
    }

    @Test
    void analyze_shouldFixUnescapedQuotesInsidePrompt() {
        // 模拟模型在 prompt 字段内输出未转义的英文双引号，导致 JSON 非法
        String promptWithRawQuotes = VALID_PROMPT.replace(
                "【语气】温和怀旧，与读者平等对话",
                "【语气】以\"你\"为主，保持\"我懂你，你也该懂\"的平视距离"
        );
        String excerpt1 = "清晨的巷子总是被豆浆的香气唤醒，老人们坐在门口闲聊。";
        String json = """
                {"excerpt1":"%s","excerpt2":"同样是编造的","prompt":"%s"}
                """.formatted(excerpt1, promptWithRawQuotes.replace("\n", "\\n"));

        SkillAnalyzeVO vo = serviceWith(json).analyze(USER_ID, ARTICLE);

        assertTrue(vo.getPrompt().contains("\"你\""), "应保留 prompt 中的示例引号");
        assertTrue(vo.getPrompt().contains("\"我懂你，你也该懂\""));
        assertEquals(excerpt1, vo.getExcerpt1());
    }

    @Test
    void analyze_shouldFallbackExcerptWhenNotVerbatim() {
        String json = """
                {"excerpt1":"这是模型编造的片段，原文里根本没有这句话。","excerpt2":"同样是编造的","prompt":"%s"}
                """.formatted(VALID_PROMPT.replace("\n", "\\n"));

        SkillAnalyzeVO vo = serviceWith(json).analyze(USER_ID, ARTICLE);

        // excerpt1 降级为首段（≤120字）；excerpt2 降级为最长句（≤80字）
        assertEquals("清晨的巷子总是被豆浆的香气唤醒，老人们坐在门口闲聊。", vo.getExcerpt1());
        assertEquals("城市的喧嚣在这里被按下了静音键，剩下的只有生活本来的模样", vo.getExcerpt2());
    }

    @Test
    void analyze_shouldFallbackExcerptWhenEmpty() {
        String json = """
                {"excerpt1":"","excerpt2":"","prompt":"%s"}
                """.formatted(VALID_PROMPT.replace("\n", "\\n"));

        SkillAnalyzeVO vo = serviceWith(json).analyze(USER_ID, ARTICLE);

        assertTrue(vo.getExcerpt1().length() <= 120 && !vo.getExcerpt1().isEmpty());
        assertTrue(vo.getExcerpt2().length() <= 80 && !vo.getExcerpt2().isEmpty());
    }

    @Test
    void analyze_shouldHandlePercentInText() {
        String textWithPercent = ARTICLE + "\n\n转化率提升 100%s 的写法不应影响模板拼接，占比 50%% 也一样。";
        SkillAnalyzeVO vo = serviceWith(VALID_JSON).analyze(USER_ID, textWithPercent);

        assertEquals(VALID_PROMPT, vo.getPrompt());
    }

    /** 正文超过 1000 字时，后端应自动截断为前 1000 字再传给 AI。 */
    @Test
    void analyze_shouldTruncateTextTo1000Chars() {
        String head = "a".repeat(800);
        String tail = "b".repeat(500);
        String longText = head + tail;

        SkillAnalyzeAiService aiService = mock(SkillAnalyzeAiService.class);
        when(aiService.call(anyString(), anyString())).thenReturn(VALID_JSON);
        SkillAnalyzeServiceImpl svc = new SkillAnalyzeServiceImpl(
                aiService, mockConfigService(), mockDailyLimiter(), mockBenefitService(), new ObjectMapper());
        svc.analyze(USER_ID, longText);

        ArgumentCaptor<String> userMsgCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).call(anyString(), userMsgCaptor.capture());
        String userMsg = userMsgCaptor.getValue();
        assertTrue(userMsg.contains(head), "应包含前 800 个 a");
        assertTrue(userMsg.contains("a".repeat(200)), "应包含第 801-1000 个 a");
        assertFalse(userMsg.contains("b".repeat(201)), "tail 应只保留前 200 个 b，不超过 200");
    }
}
