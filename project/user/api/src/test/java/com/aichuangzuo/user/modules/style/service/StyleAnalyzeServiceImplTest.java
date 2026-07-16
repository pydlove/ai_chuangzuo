package com.aichuangzuo.user.modules.style.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.style.service.impl.StyleAnalyzeServiceImpl;
import com.aichuangzuo.user.modules.style.vo.StyleAnalyzeVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * StyleAnalyzeServiceImpl 纯单测：mock AI 调用器，不起 Spring 上下文。
 */
class StyleAnalyzeServiceImplTest {

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
            {"excerpt1":"清晨的巷子总是被豆浆的香气唤醒，老人们坐在门口闲聊。","excerpt2":"慢到可以听见自己的心跳","prompt":"%s"}
            """.formatted(VALID_PROMPT.replace("\n", "\\n").replace("\"", "\\\""));

    private StyleAnalyzeServiceImpl serviceWith(String aiResponse) {
        StyleAnalyzeAiService aiService = mock(StyleAnalyzeAiService.class);
        when(aiService.call(anyString(), anyString())).thenReturn(aiResponse);
        return new StyleAnalyzeServiceImpl(aiService, new ObjectMapper());
    }

    @Test
    void analyze_shouldReturnParsedResultOnCleanJson() {
        StyleAnalyzeVO vo = serviceWith(VALID_JSON).analyze(ARTICLE);

        assertEquals("清晨的巷子总是被豆浆的香气唤醒，老人们坐在门口闲聊。", vo.getExcerpt1());
        assertEquals("慢到可以听见自己的心跳", vo.getExcerpt2());
        assertEquals(VALID_PROMPT, vo.getPrompt());
    }

    @Test
    void analyze_shouldStripCodeFence() {
        StyleAnalyzeVO vo = serviceWith("```json\n" + VALID_JSON + "\n```").analyze(ARTICLE);

        assertEquals(VALID_PROMPT, vo.getPrompt());
    }

    @Test
    void analyze_shouldThrowOnInvalidJson() {
        assertThrows(BusinessException.class, () -> serviceWith("这不是 JSON").analyze(ARTICLE));
    }

    @Test
    void analyze_shouldThrowWhenPromptMissingMarker() {
        String badPrompt = "你是一位中文写手。【语气】温和【词汇】书面【句式】短句为主，没有结构标记";
        String json = """
                {"excerpt1":"","excerpt2":"","prompt":"%s"}
                """.formatted(badPrompt);
        assertThrows(BusinessException.class, () -> serviceWith(json).analyze(ARTICLE));
    }

    @Test
    void analyze_shouldThrowWhenPromptTooLong() {
        String longPrompt = VALID_PROMPT + "长".repeat(1000);
        String json = """
                {"excerpt1":"","excerpt2":"","prompt":"%s"}
                """.formatted(longPrompt.replace("\n", "\\n"));
        assertThrows(BusinessException.class, () -> serviceWith(json).analyze(ARTICLE));
    }

    @Test
    void analyze_shouldFallbackExcerptWhenNotVerbatim() {
        String json = """
                {"excerpt1":"这是模型编造的片段，原文里根本没有这句话。","excerpt2":"同样是编造的","prompt":"%s"}
                """.formatted(VALID_PROMPT.replace("\n", "\\n"));

        StyleAnalyzeVO vo = serviceWith(json).analyze(ARTICLE);

        // excerpt1 降级为首段（≤120字）；excerpt2 降级为最长句（≤80字）
        assertEquals("清晨的巷子总是被豆浆的香气唤醒，老人们坐在门口闲聊。", vo.getExcerpt1());
        assertEquals("城市的喧嚣在这里被按下了静音键，剩下的只有生活本来的模样", vo.getExcerpt2());
    }

    @Test
    void analyze_shouldFallbackExcerptWhenEmpty() {
        String json = """
                {"excerpt1":"","excerpt2":"","prompt":"%s"}
                """.formatted(VALID_PROMPT.replace("\n", "\\n"));

        StyleAnalyzeVO vo = serviceWith(json).analyze(ARTICLE);

        assertTrue(vo.getExcerpt1().length() <= 120 && !vo.getExcerpt1().isEmpty());
        assertTrue(vo.getExcerpt2().length() <= 80 && !vo.getExcerpt2().isEmpty());
    }
}
