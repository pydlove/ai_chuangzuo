# 风格学习 AI 分析 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把「学习的风格」的前端 mock 分析替换为后端真实 AI 分析（spec: `docs/superpowers/specs/2026-07-16-style-analyze-ai-design.md`）。

**Architecture:** user/api 新增 `POST /api/v1/user/styles/analyze` 同步接口：`StyleAnalyzeAiService` 读 `a_model_config` active 模型调大模型（仿 `TitleOptimizeAiService`，temperature=0.3），`StyleAnalyzeServiceImpl` 负责 prompt 模板、JSON 解析、excerpt 逐字校验与降级。前端仅替换 `useStyles.js` 的 `analyzeArticleStyle` 函数体为接口调用，删除 mock 死代码。

**Tech Stack:** JDK 17 + Spring Boot + MyBatis-Plus + Lombok；JUnit 5 + Mockito（纯单测，不起 Spring 上下文）；前端 Vue 3 + axios（ant-design-vue message）。

## Global Constraints

- 错误码段 112xxx：新增 `STYLE_ANALYZE_FAILED(112006, "风格分析失败，请重试")`，不得占用 112001-112005。
- 模型调用 `temperature=0.3`、`max_tokens=4096`、`response_format=json_object`、connect 10s / read 60s，与 `TitleOptimizeAiService` 一致。
- prompt 结尾「最终输出要求」四条规定逐字固定（见 Task 2 代码），是项目所有 JSON 返回 prompt 的固定后缀。
- 前端 axios 实例默认 timeout 10 秒（`src/utils/request.js`），analyze 请求**必须单独传 90 秒**超时，否则 AI 分析必然超时。
- 接口路径 `/api/v1/user/styles/analyze`，鉴权沿用 SecurityConfig 现状，不加权益门。
- 保存链路不改：结果页仍走现有 `POST /styles`（`sourceType=2`），excerpt 不入库。
- CLAUDE.md 清理原则：mock 死代码（段落抽取、固定模板、setTimeout 延迟）必须删干净。
- 提交信息遵循仓库惯例：`type(scope): 中文描述`（如 `feat(user-api): ...`）。

---

### Task 1: StyleAnalyzeAiService（模型调用器）

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/service/StyleAnalyzeAiService.java`
- Reference（照抄骨架）: `project/user/api/src/main/java/com/aichuangzuo/user/modules/article/service/TitleOptimizeAiService.java`

**Interfaces:**
- Consumes: `ArticleModelConfigMapper.selectActive()` → `ActiveModelConfig`（已有）；`AesUtil.decrypt(String, String)`（已有）；`StyleErrorCode.STYLE_ANALYZE_FAILED`（Task 2 才定义，本任务先定义错误码见 Step 1）
- Produces: `StyleAnalyzeAiService.call(String systemMessage, String userMessage): String` — 返回 assistant content 原文，失败抛 `BusinessException(STYLE_ANALYZE_FAILED)`

- [ ] **Step 1: 新增错误码**

修改 `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/enums/StyleErrorCode.java`，在 `STYLE_SCOPE_TOO_LONG` 后追加：

```java
    STYLE_SCOPE_TOO_LONG(112005, "适用范围标签过多或过长"),
    STYLE_ANALYZE_FAILED(112006, "风格分析失败，请重试");
```

- [ ] **Step 2: 创建 StyleAnalyzeAiService**

```java
package com.aichuangzuo.user.modules.style.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.utils.AesUtil;
import com.aichuangzuo.user.modules.article.dto.ActiveModelConfig;
import com.aichuangzuo.user.modules.article.mapper.ArticleModelConfigMapper;
import com.aichuangzuo.user.modules.style.enums.StyleErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 风格分析的模型调用器：读 a_model_config 选 endpoint，发送 system + user messages。
 *
 * <p>仿 {@code TitleOptimizeAiService}，唯一差异：temperature=0.3（分析任务要稳定，标题优化是 0.8）。
 */
@Slf4j
@Service
public class StyleAnalyzeAiService {

    private final ArticleModelConfigMapper modelConfigMapper;
    private final String apiKeySecret;
    private final ObjectMapper objectMapper;

    public StyleAnalyzeAiService(ArticleModelConfigMapper modelConfigMapper,
                                 @Value("${user.model.api-key-secret}") String apiKeySecret) {
        this.modelConfigMapper = modelConfigMapper;
        this.apiKeySecret = apiKeySecret;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用当前 active 模型，返回 assistant content 原文。
     *
     * @throws BusinessException STYLE_ANALYZE_FAILED 配置缺失/传输失败/响应解析失败
     */
    public String call(String systemMessage, String userMessage) {
        ActiveModelConfig cfg = modelConfigMapper.selectActive();
        if (cfg == null) {
            log.warn("AI 风格分析失败：无 active 模型配置");
            throw new BusinessException(StyleErrorCode.STYLE_ANALYZE_FAILED);
        }
        String apiKey;
        try {
            apiKey = AesUtil.decrypt(cfg.getApiKeyEncrypted(), apiKeySecret);
        } catch (Exception e) {
            log.warn("AI 风格分析 api key 解密失败 modelConfigId={}", cfg.getId(), e);
            throw new BusinessException(StyleErrorCode.STYLE_ANALYZE_FAILED);
        }

        String url = resolveUrl(cfg);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", cfg.getModelCode());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemMessage),
                Map.of("role", "user", "content", userMessage)
        ));
        body.put("temperature", 0.3);
        body.put("max_tokens", 4096);
        body.put("top_p", 1.0);
        body.put("stream", false);
        body.put("response_format", Map.of("type", "json_object"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);

        try {
            ResponseEntity<String> response = new RestTemplate(factory).exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            return extractContent(response.getBody(), cfg.getProviderType());
        } catch (RestClientException e) {
            log.warn("AI 风格分析调用失败 provider={} msg={}", cfg.getProviderType(), e.getMessage());
            throw new BusinessException(StyleErrorCode.STYLE_ANALYZE_FAILED);
        }
    }

    private String resolveUrl(ActiveModelConfig cfg) {
        String base = cfg.getBaseUrl() == null ? "" : cfg.getBaseUrl().trim().replaceAll("/+$", "");
        int schemeEnd = base.indexOf("://");
        if (schemeEnd >= 0) {
            int pathStart = base.indexOf('/', schemeEnd + 3);
            if (pathStart > 0) base = base.substring(0, pathStart);
        }
        String suffix = "minimax".equalsIgnoreCase(cfg.getProviderType())
                ? "/v1/text/chatcompletion_v2"
                : "/v1/chat/completions";
        return base + suffix;
    }

    private String extractContent(String responseBody, String providerType) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText("");
                if (!content.isEmpty()) {
                    return content;
                }
                log.warn("AI 风格分析返回 content 为空 provider={} finish_reason={}",
                        providerType, choices.get(0).path("finish_reason").asText(""));
            }
        } catch (Exception e) {
            log.warn("AI 风格分析响应解析失败", e);
        }
        throw new BusinessException(StyleErrorCode.STYLE_ANALYZE_FAILED);
    }
}
```

- [ ] **Step 3: 编译验证**

Run: `cd project/user/api && mvn -q compile`
Expected: BUILD SUCCESS（无输出或仅 warning）

- [ ] **Step 4: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/style/service/StyleAnalyzeAiService.java project/user/api/src/main/java/com/aichuangzuo/user/modules/style/enums/StyleErrorCode.java
git commit -m "feat(user-api): 风格分析 AI 调用器（仿标题优化，temperature=0.3）"
```

---

### Task 2: StyleAnalyzeService 核心逻辑（TDD）

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/vo/StyleAnalyzeVO.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/service/StyleAnalyzeService.java`
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/service/impl/StyleAnalyzeServiceImpl.java`
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/modules/style/service/StyleAnalyzeServiceImplTest.java`

**Interfaces:**
- Consumes: `StyleAnalyzeAiService.call(String, String): String`（Task 1）
- Produces: `StyleAnalyzeService.analyze(String text): StyleAnalyzeVO`；`StyleAnalyzeVO` 字段 `excerpt1 / excerpt2 / prompt`（均 String）— Task 3 Controller 与前端共用此形状

- [ ] **Step 1: 创建 StyleAnalyzeVO**

```java
package com.aichuangzuo.user.modules.style.vo;

import lombok.Data;

/**
 * 风格分析结果。
 */
@Data
public class StyleAnalyzeVO {

    /** 原文代表性片段 1（≤120字；模型未逐字命中原文时降级为首段截取）。 */
    private String excerpt1;

    /** 原文代表性片段 2（≤80字；模型未逐字命中原文时降级为最长句截取）。 */
    private String excerpt2;

    /** 四段式风格提示词（≤1000字，可直接入库 u_user_style.prompt）。 */
    private String prompt;
}
```

- [ ] **Step 2: 创建 StyleAnalyzeService 接口**

```java
package com.aichuangzuo.user.modules.style.service;

import com.aichuangzuo.user.modules.style.vo.StyleAnalyzeVO;

/**
 * 风格分析服务：调大模型拆解参考文章的写作风格。
 */
public interface StyleAnalyzeService {

    /**
     * 分析参考文章风格。
     *
     * @param text 参考文章正文（200-3000 字，Controller 层已校验）
     * @return 风格提示词 + 2 段原文摘录
     */
    StyleAnalyzeVO analyze(String text);
}
```

- [ ] **Step 3: 写失败测试**

创建 `project/user/api/src/test/java/com/aichuangzuo/user/modules/style/service/StyleAnalyzeServiceImplTest.java`：

```java
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
```

- [ ] **Step 4: 跑测试确认全部失败（类不存在）**

Run: `cd project/user/api && mvn -q test -Dtest=StyleAnalyzeServiceImplTest`
Expected: 编译失败，`StyleAnalyzeServiceImpl` 不存在

- [ ] **Step 5: 创建 StyleAnalyzeServiceImpl**

```java
package com.aichuangzuo.user.modules.style.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.style.enums.StyleErrorCode;
import com.aichuangzuo.user.modules.style.service.StyleAnalyzeAiService;
import com.aichuangzuo.user.modules.style.service.StyleAnalyzeService;
import com.aichuangzuo.user.modules.style.vo.StyleAnalyzeVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * AI 风格分析服务实现。
 *
 * <p>prompt 模板与降级策略见 docs/superpowers/specs/2026-07-16-style-analyze-ai-design.md 第 4、5 节。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StyleAnalyzeServiceImpl implements StyleAnalyzeService {

    private static final int PROMPT_MAX_LENGTH = 1000;
    private static final int EXCERPT1_MAX = 120;
    private static final int EXCERPT2_MAX = 80;
    private static final List<String> REQUIRED_MARKERS = List.of("【语气】", "【词汇】", "【句式】", "【结构】");

    private static final String SYSTEM_MESSAGE =
            "你是一位资深的中文文体分析师，擅长拆解中文自媒体文章的写作风格，并把风格特征提炼成可直接指导 AI 写作的提示词。";

    /** 用户消息模板：%s 为参考文章正文。 */
    private static final String USER_PROMPT_TEMPLATE = """
            请分析以下参考文章的写作风格，完成两件事：

            【文章正文】
            %s

            【任务】
            1. 从【语气】【词汇】【句式】【结构】四个维度拆解风格特征。每条特征必须具体、可模仿，禁止空泛形容（不要写"语言优美"，要写"多用15字以内短句，段间留白多"这类可执行描述）。
            2. 从原文中逐字摘录 2 个最能代表该风格的片段。

            【输出 JSON 结构】
            {"excerpt1":"原文中最能代表风格的连续片段，不超过120字，必须逐字摘自原文","excerpt2":"另一个代表性片段，不超过80字，必须逐字摘自原文，且不与excerpt1重复","prompt":"不超过800字的风格提示词"}

            其中 prompt 字段严格使用以下模板：
            你是一位中文写手，请模仿以下参考文章的写作风格：

            【语气】（人称视角、情感温度、与读者的距离感，1-2句）
            【词汇】（书面/口语倾向、网络用语与语气词的使用习惯，1-2句）
            【句式】（句子长短与节奏、标点习惯、常用修辞，1-2句）
            【结构】（开头方式、段落组织、结尾处理，1-2句）

            请在生成新内容时严格遵循以上风格特征。

            最终输出要求（覆盖以上所有说明，必须严格遵守）：
              1. 只输出一个合法 JSON 对象。不要任何前言、说明、免责声明、思路解释、markdown 标题或后记。
              2. 不要用 ```json 或任何代码围栏包裹。
              3. 第一个字符必须是 {，最后一个字符必须是 }。
              4. 所有需要解释、标注、声明的信息，必须放进 JSON 字段里，不能写在 JSON 之外。
            """;

    private final StyleAnalyzeAiService aiService;
    private final ObjectMapper objectMapper;

    @Override
    public StyleAnalyzeVO analyze(String text) {
        String aiResp = aiService.call(SYSTEM_MESSAGE, String.format(USER_PROMPT_TEMPLATE, text));
        JsonNode root = parseJson(stripCodeFence(aiResp));

        String prompt = root.path("prompt").asText("").trim();
        validatePrompt(prompt);

        StyleAnalyzeVO vo = new StyleAnalyzeVO();
        vo.setPrompt(prompt);
        vo.setExcerpt1(resolveExcerpt(root.path("excerpt1").asText(""), text, true));
        vo.setExcerpt2(resolveExcerpt(root.path("excerpt2").asText(""), text, false));
        return vo;
    }

    private JsonNode parseJson(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            log.warn("AI 风格分析结果解析失败 resp={}", abbreviate(raw));
            throw new BusinessException(StyleErrorCode.STYLE_ANALYZE_FAILED);
        }
    }

    /** prompt 非空、≤1000 字、含四个维度标记，缺一不可。 */
    private void validatePrompt(String prompt) {
        boolean valid = !prompt.isEmpty()
                && prompt.length() <= PROMPT_MAX_LENGTH
                && REQUIRED_MARKERS.stream().allMatch(prompt::contains);
        if (!valid) {
            log.warn("AI 风格分析 prompt 校验失败 length={}", prompt.length());
            throw new BusinessException(StyleErrorCode.STYLE_ANALYZE_FAILED);
        }
    }

    /**
     * 摘录必须逐字摘自原文（防模型编造）；未命中时降级：
     * excerpt1 → 首个长度 > 20 字段落截取 120 字；excerpt2 → 最长句截取 80 字。
     */
    private String resolveExcerpt(String excerpt, String text, boolean firstParagraph) {
        String candidate = excerpt == null ? "" : excerpt.trim();
        if (!candidate.isEmpty() && text.contains(candidate)) {
            return candidate;
        }
        if (firstParagraph) {
            String first = Arrays.stream(text.split("\\n\\s*\\n"))
                    .map(String::trim)
                    .filter(p -> p.length() > 20)
                    .findFirst()
                    .orElse("");
            return first.length() <= EXCERPT1_MAX ? first : first.substring(0, EXCERPT1_MAX);
        }
        return Arrays.stream(text.split("[。！？\\n]"))
                .map(String::trim)
                .filter(s -> s.length() > 10)
                .max(Comparator.comparingInt(String::length))
                .map(s -> s.length() <= EXCERPT2_MAX ? s : s.substring(0, EXCERPT2_MAX))
                .orElse("");
    }

    /** 防御：模型偶有 ```json 围栏输出，剥掉再解析。 */
    private static String stripCodeFence(String text) {
        if (text == null) {
            return "";
        }
        String s = text.strip();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline > 0) {
                s = s.substring(firstNewline + 1);
            }
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3);
            }
        }
        return s.strip();
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return "null";
        }
        return s.length() <= 200 ? s : s.substring(0, 200) + "...";
    }
}
```

- [ ] **Step 6: 跑测试确认全部通过**

Run: `cd project/user/api && mvn -q test -Dtest=StyleAnalyzeServiceImplTest`
Expected: `Tests run: 7, Failures: 0, Errors: 0`

- [ ] **Step 7: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/style/vo/StyleAnalyzeVO.java project/user/api/src/main/java/com/aichuangzuo/user/modules/style/service/StyleAnalyzeService.java project/user/api/src/main/java/com/aichuangzuo/user/modules/style/service/impl/StyleAnalyzeServiceImpl.java project/user/api/src/test/java/com/aichuangzuo/user/modules/style/service/StyleAnalyzeServiceImplTest.java
git commit -m "feat(user-api): 风格分析服务 — prompt 模板 + 解析校验降级 + 单测"
```

---

### Task 3: Controller 端点

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/dto/request/AnalyzeStyleRequest.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/style/controller/UserStyleController.java`

**Interfaces:**
- Consumes: `StyleAnalyzeService.analyze(String): StyleAnalyzeVO`（Task 2）
- Produces: `POST /api/v1/user/styles/analyze`，请求 `{"text": "..."}`，响应 `Result<StyleAnalyzeVO>` — 前端 Task 4 消费

- [ ] **Step 1: 创建 AnalyzeStyleRequest**

```java
package com.aichuangzuo.user.modules.style.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 风格分析请求。
 */
@Data
public class AnalyzeStyleRequest {

    @NotBlank(message = "参考文章正文不能为空")
    @Size(min = 200, max = 3000, message = "参考文章正文长度需为 200-3000 字符")
    private String text;
}
```

- [ ] **Step 2: UserStyleController 新增端点**

import 追加：

```java
import com.aichuangzuo.user.modules.style.dto.request.AnalyzeStyleRequest;
import com.aichuangzuo.user.modules.style.service.StyleAnalyzeService;
import com.aichuangzuo.user.modules.style.vo.StyleAnalyzeVO;
```

构造注入字段追加（`@RequiredArgsConstructor` 已有，直接加字段即可）：

```java
    private final StyleAnalyzeService styleAnalyzeService;
```

在 `listSystemStyles` 方法后追加端点方法：

```java
    /**
     * AI 分析参考文章的写作风格，返回风格提示词与 2 段原文摘录。
     *
     * @param request 含参考文章正文（200-3000 字）
     * @return 分析结果（excerpt 仅供展示，不入库）
     */
    @Operation(summary = "AI 分析参考文章风格")
    @PostMapping("/analyze")
    public Result<StyleAnalyzeVO> analyzeStyle(@Valid @RequestBody AnalyzeStyleRequest request) {
        return Result.success(styleAnalyzeService.analyze(request.getText().trim()));
    }
```

- [ ] **Step 3: 编译验证**

Run: `cd project/user/api && mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: 回归既有风格测试**

Run: `cd project/user/api && mvn -q test -Dtest=UserStyleServiceTest`
Expected: 全部通过（确认 Controller 构造器变更未破坏既有注入）

- [ ] **Step 5: Commit**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/style/dto/request/AnalyzeStyleRequest.java project/user/api/src/main/java/com/aichuangzuo/user/modules/style/controller/UserStyleController.java
git commit -m "feat(user-api): 新增 POST /styles/analyze 风格分析接口"
```

---

### Task 4: 前端接入 + 删 mock

**Files:**
- Modify: `project/user/web/src/api/style.js`
- Modify: `project/user/web/src/composables/useStyles.js`
- Modify: `project/user/web/src/views/console/StylesIndex.vue`（仅 `runAnalysis` 加错误处理）

**Interfaces:**
- Consumes: `POST /api/v1/user/styles/analyze`（Task 3），响应 `{code:0, data:{excerpt1, excerpt2, prompt}}`
- Produces: `analyzeArticleStyle(text, meta)` 返回形状不变（`{sourceType, excerpt1, excerpt2, prompt, scope, createdAt}`），StylesIndex.vue 调用方零改动

- [ ] **Step 1: api/style.js 新增 analyzeStyle（必须带 90s 超时）**

文件末尾追加：

```js
/**
 * AI 分析参考文章风格。
 * 注意：AI 分析约 10-30 秒，axios 实例默认 timeout 10s 必然超时，必须单独传 90s。
 * @param {string} text 参考文章正文（200-3000 字）
 * @returns {Promise<{code:number, data:{excerpt1:string, excerpt2:string, prompt:string}}>}
 */
export function analyzeStyle(text) {
  return api.post('/styles/analyze', { text }, { timeout: 90000 })
}
```

- [ ] **Step 2: useStyles.js 替换 analyzeArticleStyle，删除 mock**

import 行改为：

```js
import { getMyStyles, createStyle, updateStyle, deleteStyle, getSystemStyles, analyzeStyle } from '@/api/style'
```

`analyzeArticleStyle` 整个函数（含 mock 的段落抽取、最长句查找、固定模板、setTimeout）替换为：

```js
// 风格分析（后端 AI 分析）
export async function analyzeArticleStyle(text, meta) {
  isLearning.value = true
  try {
    const res = await analyzeStyle(text)
    const data = res.data || res || {}
    return {
      sourceType: meta.sourceType,
      excerpt1: data.excerpt1 || '',
      excerpt2: data.excerpt2 || '',
      prompt: data.prompt || '',
      scope: '',     // 适用范围，由用户在结果页手填
      createdAt: new Date().toISOString()
    }
  } finally {
    isLearning.value = false
  }
}
```

确认删除干净：函数内不再出现 `paragraphs` / `longest` / `setTimeout` / 固定四段模板字符串。`grep -n "setTimeout\|mock" project/user/web/src/composables/useStyles.js` 应无结果。

- [ ] **Step 3: StylesIndex.vue 的 runAnalysis 加错误处理**

现状 `runAnalysis`（约 878 行）无 try/catch，AI 失败会变成未处理 Promise  rejection。替换为：

```js
const runAnalysis = async (text, sourceType) => {
  try {
    const tempResult = await analyzeArticleStyle(text, { sourceType })
    learnedResult.value = { ...tempResult, name: '' }
  } catch (err) {
    message.error(err?.message || '分析失败，请重试')
  }
}
```

确认 `message` 已从 `ant-design-vue` import（若没有则补 `import { message } from 'ant-design-vue'`）。

- [ ] **Step 4: 构建验证**

Run: `cd project/user/web && npm run build`
Expected: 构建成功，无报错

- [ ] **Step 5: Commit**

```bash
git add project/user/web/src/api/style.js project/user/web/src/composables/useStyles.js project/user/web/src/views/console/StylesIndex.vue
git commit -m "feat(user-web): 学习风格接入后端 AI 分析，删除前端 mock"
```

---

### Task 5: 全量回归 + 手动 E2E

**Files:**
- 无新增/修改，仅验证

**Interfaces:**
- Consumes: Task 1-4 全部产出

- [ ] **Step 1: 后端全量测试**

Run: `cd project/user/api && mvn -q test`
Expected: 全部通过，无 FAIL/ERROR

- [ ] **Step 2: 前端构建**

Run: `cd project/user/web && npm run build`
Expected: 构建成功

- [ ] **Step 3: 手动 E2E（需本地 MySQL + a_model_config 有 active 配置）**

1. 启动后端：`cd project/user/api && mvn spring-boot:run`
2. 启动前端：`cd project/user/web && npm run dev`
3. 登录 → 我的风格 → 「学习的风格」tab → 「+ 学习新风格」
4. 粘贴一篇 2000 字左右的公众号文章 → 「开始学习」→ 观察进度态 10-30 秒
5. 预期结果页：提示词含【语气】【词汇】【句式】【结构】四段；2 段摘录确为原文片段
6. 填名称（如「我的公众号风」）+ 适用范围 → 「保存到风格库」→ 卡片出现
7. 创作页风格弹框第三个 tab 能看到该卡片，点「使用」可选用
8. 异常路径：把后端 `a_model_config` 的 api key 改错 → 再分析 → 前端 toast「分析失败，请重试」且对话框回到输入态（测完改回）

- [ ] **Step 4: 更新进度 ledger**

若 `.superpowers/sdd/progress.md` 存在对应条目，追加完成记录（任务名、完成日期、验证结果）。

---

## Self-Review 记录

- **Spec 覆盖**：spec 第 3 节接口 → Task 3；第 4 节组件 → Task 1/2/3；第 5 节提示词 → Task 2（USER_PROMPT_TEMPLATE 逐字一致，含「最终输出要求」四条）；第 6 节前端改动 → Task 4；第 7 节错误处理 → Task 2 单测 + Task 4 Step 3 + Task 5 Step 3-8；第 9 节测试要点 → Task 2 单测 7 条 + Task 5 E2E。
- **类型一致性**：`StyleAnalyzeService.analyze(String): StyleAnalyzeVO`、`StyleAnalyzeAiService.call(String,String): String`、`StyleAnalyzeVO{excerpt1,excerpt2,prompt}` 在 Task 2 定义、Task 3/4 消费，一致。
- **已知偏差**：spec 第 6 节提到删 `simpleHash`/`findLearnedStyleByHash`，实际代码里这两个函数从未落地（实现时已去掉 hash 去重），无需删除；以 Task 4 为准。
