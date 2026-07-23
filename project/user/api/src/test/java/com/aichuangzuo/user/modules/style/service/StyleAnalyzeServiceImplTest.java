package com.aichuangzuo.user.modules.style.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
import com.aichuangzuo.user.modules.style.service.impl.StyleAnalyzeServiceImpl;
import com.aichuangzuo.user.modules.style.vo.StyleAnalyzeVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * StyleAnalyzeServiceImpl 纯单测：mock AI 调用器 + mock BenefitService，不起 Spring 上下文。
 */
class StyleAnalyzeServiceImplTest {

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
            {"excerpt1":"清晨的巷子总是被豆浆的香气唤醒，老人们坐在门口闲聊。","excerpt2":"慢到可以听见自己的心跳","prompt":"%s"}
            """.formatted(VALID_PROMPT.replace("\n", "\\n").replace("\"", "\\\""));

    /** 构造一个 BenefitService mock：默认任何 userId 都允许消费额度。 */
    private BenefitService mockBenefitServiceAllowed() {
        BenefitService bs = mock(BenefitService.class);
        BenefitCheckVO vo = new BenefitCheckVO();
        vo.setAllowed(true);
        when(bs.consume(anyLong(), anyString())).thenReturn(vo);
        return bs;
    }

    private StyleAnalyzeServiceImpl serviceWith(String aiResponse) {
        return serviceWith(aiResponse, mockBenefitServiceAllowed());
    }

    private StyleAnalyzeServiceImpl serviceWith(String aiResponse, BenefitService benefitService) {
        StyleAnalyzeAiService aiService = mock(StyleAnalyzeAiService.class);
        when(aiService.call(anyString(), anyString())).thenReturn(aiResponse);
        return new StyleAnalyzeServiceImpl(aiService, benefitService, new ObjectMapper());
    }

    @Test
    void analyze_shouldReturnParsedResultOnCleanJson() {
        StyleAnalyzeVO vo = serviceWith(VALID_JSON).analyze(USER_ID, ARTICLE);

        assertEquals("清晨的巷子总是被豆浆的香气唤醒，老人们坐在门口闲聊。", vo.getExcerpt1());
        assertEquals("慢到可以听见自己的心跳", vo.getExcerpt2());
        assertEquals(VALID_PROMPT, vo.getPrompt());
    }

    @Test
    void analyze_shouldStripCodeFence() {
        StyleAnalyzeVO vo = serviceWith("```json\n" + VALID_JSON + "\n```").analyze(USER_ID, ARTICLE);

        assertEquals(VALID_PROMPT, vo.getPrompt());
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
        String longPrompt = VALID_PROMPT + "长".repeat(1000);
        String json = """
                {"excerpt1":"","excerpt2":"","prompt":"%s"}
                """.formatted(longPrompt.replace("\n", "\\n"));
        assertThrows(BusinessException.class, () -> serviceWith(json).analyze(USER_ID, ARTICLE));
    }

    @Test
    void analyze_shouldFallbackExcerptWhenNotVerbatim() {
        String json = """
                {"excerpt1":"这是模型编造的片段，原文里根本没有这句话。","excerpt2":"同样是编造的","prompt":"%s"}
                """.formatted(VALID_PROMPT.replace("\n", "\\n"));

        StyleAnalyzeVO vo = serviceWith(json).analyze(USER_ID, ARTICLE);

        // excerpt1 降级为首段（≤120字）；excerpt2 降级为最长句（≤80字）
        assertEquals("清晨的巷子总是被豆浆的香气唤醒，老人们坐在门口闲聊。", vo.getExcerpt1());
        assertEquals("城市的喧嚣在这里被按下了静音键，剩下的只有生活本来的模样", vo.getExcerpt2());
    }

    @Test
    void analyze_shouldFallbackExcerptWhenEmpty() {
        String json = """
                {"excerpt1":"","excerpt2":"","prompt":"%s"}
                """.formatted(VALID_PROMPT.replace("\n", "\\n"));

        StyleAnalyzeVO vo = serviceWith(json).analyze(USER_ID, ARTICLE);

        assertTrue(vo.getExcerpt1().length() <= 120 && !vo.getExcerpt1().isEmpty());
        assertTrue(vo.getExcerpt2().length() <= 80 && !vo.getExcerpt2().isEmpty());
    }

    @Test
    void analyze_shouldHandlePercentInText() {
        String textWithPercent = ARTICLE + "\n\n转化率提升 100%s 的写法不应影响模板拼接，占比 50%% 也一样。";
        StyleAnalyzeVO vo = serviceWith(VALID_JSON).analyze(USER_ID, textWithPercent);

        assertEquals(VALID_PROMPT, vo.getPrompt());
    }

    /** 额度不足（basic=0 / pro/flagship 用满）应当阻断：抛 BusinessException 而不再调 AI。 */
    @Test
    void analyze_shouldThrowWhenQuotaExhausted() {
        BenefitService bs = mock(BenefitService.class);
        doThrow(new BusinessException(com.aichuangzuo.user.modules.benefit.enums.BenefitErrorCode.QUOTA_EXHAUSTED))
                .when(bs).consume(anyLong(), ArgumentMatchers.eq("style_learn_analyze"));

        StyleAnalyzeAiService aiService = mock(StyleAnalyzeAiService.class);
        // 若 AI 被错误调用就 fail
        when(aiService.call(anyString(), anyString())).thenThrow(new AssertionError("AI 不应在额度不足时被调用"));

        StyleAnalyzeServiceImpl svc = new StyleAnalyzeServiceImpl(aiService, bs, new ObjectMapper());
        assertThrows(BusinessException.class, () -> svc.analyze(USER_ID, ARTICLE));
    }

    /** 额度校验放行后必须实际触发消费（含写入 u_benefit_usage）。 */
    @Test
    void analyze_shouldConsumeQuotaBeforeCallingAi() {
        BenefitService bs = mock(BenefitService.class);
        BenefitCheckVO vo = new BenefitCheckVO();
        vo.setAllowed(true);
        when(bs.consume(anyLong(), ArgumentMatchers.eq("style_learn_analyze"))).thenReturn(vo);

        StyleAnalyzeAiService aiService = mock(StyleAnalyzeAiService.class);
        when(aiService.call(anyString(), anyString())).thenReturn(VALID_JSON);

        StyleAnalyzeServiceImpl svc = new StyleAnalyzeServiceImpl(aiService, bs, new ObjectMapper());
        svc.analyze(USER_ID, ARTICLE);

        org.mockito.Mockito.verify(bs).consume(anyLong(), ArgumentMatchers.eq("style_learn_analyze"));
    }

    /** AI 调用失败时应当退回额度，避免学习失败也扣次数。 */
    @Test
    void analyze_shouldRefundQuotaWhenAiFails() {
        BenefitService bs = mockBenefitServiceForRefundTest();

        StyleAnalyzeAiService aiService = mock(StyleAnalyzeAiService.class);
        when(aiService.call(anyString(), anyString())).thenThrow(new BusinessException(
                com.aichuangzuo.user.modules.benefit.enums.BenefitErrorCode.QUOTA_EXHAUSTED));

        StyleAnalyzeServiceImpl svc = new StyleAnalyzeServiceImpl(aiService, bs, new ObjectMapper());
        assertThrows(BusinessException.class, () -> svc.analyze(USER_ID, ARTICLE));

        verify(bs).consume(anyLong(), ArgumentMatchers.eq("style_learn_analyze"));
        verify(bs).refund(anyLong(), ArgumentMatchers.eq("style_learn_analyze"));
    }

    /** AI 返回无法解析的响应时应当退回额度。 */
    @Test
    void analyze_shouldRefundQuotaWhenJsonInvalid() {
        BenefitService bs = mockBenefitServiceForRefundTest();

        StyleAnalyzeAiService aiService = mock(StyleAnalyzeAiService.class);
        when(aiService.call(anyString(), anyString())).thenReturn("这不是 JSON");

        StyleAnalyzeServiceImpl svc = new StyleAnalyzeServiceImpl(aiService, bs, new ObjectMapper());
        assertThrows(BusinessException.class, () -> svc.analyze(USER_ID, ARTICLE));

        verify(bs).consume(anyLong(), ArgumentMatchers.eq("style_learn_analyze"));
        verify(bs).refund(anyLong(), ArgumentMatchers.eq("style_learn_analyze"));
    }

    /** AI 返回的 prompt 校验不通过时应当退回额度。 */
    @Test
    void analyze_shouldRefundQuotaWhenPromptInvalid() {
        BenefitService bs = mockBenefitServiceForRefundTest();

        String badPrompt = "你是一位中文写手。【语气】温和【词汇】书面【句式】短句为主，没有结构标记";
        String json = """
                {"excerpt1":"","excerpt2":"","prompt":"%s"}
                """.formatted(badPrompt);
        StyleAnalyzeAiService aiService = mock(StyleAnalyzeAiService.class);
        when(aiService.call(anyString(), anyString())).thenReturn(json);

        StyleAnalyzeServiceImpl svc = new StyleAnalyzeServiceImpl(aiService, bs, new ObjectMapper());
        assertThrows(BusinessException.class, () -> svc.analyze(USER_ID, ARTICLE));

        verify(bs).consume(anyLong(), ArgumentMatchers.eq("style_learn_analyze"));
        verify(bs).refund(anyLong(), ArgumentMatchers.eq("style_learn_analyze"));
    }

    /** 成功时不应退回额度。 */
    @Test
    void analyze_shouldNotRefundQuotaOnSuccess() {
        BenefitService bs = mockBenefitServiceForRefundTest();

        StyleAnalyzeAiService aiService = mock(StyleAnalyzeAiService.class);
        when(aiService.call(anyString(), anyString())).thenReturn(VALID_JSON);

        StyleAnalyzeServiceImpl svc = new StyleAnalyzeServiceImpl(aiService, bs, new ObjectMapper());
        svc.analyze(USER_ID, ARTICLE);

        verify(bs).consume(anyLong(), ArgumentMatchers.eq("style_learn_analyze"));
        verify(bs, never()).refund(anyLong(), ArgumentMatchers.eq("style_learn_analyze"));
    }

    private BenefitService mockBenefitServiceForRefundTest() {
        BenefitService bs = mock(BenefitService.class);
        BenefitCheckVO vo = new BenefitCheckVO();
        vo.setAllowed(true);
        when(bs.consume(anyLong(), ArgumentMatchers.eq("style_learn_analyze"))).thenReturn(vo);
        return bs;
    }
}
