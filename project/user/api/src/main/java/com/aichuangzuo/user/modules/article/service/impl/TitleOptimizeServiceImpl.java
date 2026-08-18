package com.aichuangzuo.user.modules.article.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.vo.AiPromptRendered;
import com.aichuangzuo.user.modules.article.entity.Article;
import com.aichuangzuo.user.modules.article.enums.ArticleErrorCode;
import com.aichuangzuo.user.modules.article.mapper.ArticleMapper;
import com.aichuangzuo.user.modules.article.service.TitleOptimizeAiService;
import com.aichuangzuo.user.modules.article.service.TitleOptimizeService;
import com.aichuangzuo.user.modules.article.vo.TitleOptimizeVO;
import com.aichuangzuo.user.modules.benefit.enums.BenefitErrorCode;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
import com.aichuangzuo.user.modules.aiprompt.service.AiPromptRenderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 标题优化服务实现。
 *
 * <p>权益门：boolean 权益 ai_title_optimize（basic 不可用，pro/flagship 可用）。
 * 缓存：首次生成写 u_article.optimized_titles_json，之后永远返回首次结果。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TitleOptimizeServiceImpl implements TitleOptimizeService {

    /** 权益编码：AI 标题优化。 */
    private static final String TITLE_OPTIMIZE_BENEFIT = "ai_title_optimize";

    /** 参与优化的平台（顺序即前端 tab 顺序）。 */
    private static final List<String> PLATFORMS = List.of(
            "wechat", "xiaohongshu", "toutiao", "baijiahao", "zhihu", "douyin", "bilibili");

    /** 每个平台的标题条数。 */
    private static final int TITLES_PER_PLATFORM = 2;

    /** 送给大模型的正文最大长度，超出截断，控制 token。 */
    private static final int BODY_EXCERPT_MAX = 1500;

    private final ArticleMapper articleMapper;
    private final BenefitService benefitService;
    private final TitleOptimizeAiService aiService;
    private final AiPromptRenderService aiPromptRenderService;
    private final ObjectMapper objectMapper;

    @Override
    public TitleOptimizeVO optimize(Long userId, String bizNo) {
        BenefitCheckVO benefit = benefitService.check(userId, TITLE_OPTIMIZE_BENEFIT);
        if (!Boolean.TRUE.equals(benefit.getAllowed())) {
            throw new BusinessException(BenefitErrorCode.BENEFIT_NOT_SUPPORTED);
        }

        Article article = mustFind(userId, bizNo);

        // 命中缓存：永远返回首次生成的结果
        Map<String, List<String>> cached = parseTitles(article.getOptimizedTitlesJson());
        if (cached != null) {
            return toVo(cached, true);
        }

        AiPromptRendered prompt = aiPromptRenderService.render("title_optimize_v1",
                Map.of("title", article.getTitle(), "bodyExcerpt", excerpt(article.getBody())));
        String aiResp = aiService.call(prompt.systemRole(), prompt.userPrompt());
        Map<String, List<String>> titles = parseTitles(stripCodeFence(aiResp));
        if (titles == null || titles.isEmpty()) {
            log.warn("AI 标题优化结果解析失败 bizNo={}, resp={}", bizNo, abbreviate(aiResp));
            throw new BusinessException(ArticleErrorCode.TITLE_OPTIMIZE_FAILED);
        }

        // 并发首次点击兜底：仅当缓存仍为空才写入，避免重复覆盖
        // 存储结构与模型输出一致（含 titles 包裹层），缓存命中时走同一解析路径
        try {
            String json = objectMapper.writeValueAsString(Map.of("titles", titles));
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getId, article.getId())
                    .isNull(Article::getOptimizedTitlesJson)
                    .set(Article::getOptimizedTitlesJson, json));
        } catch (Exception e) {
            log.warn("AI 标题优化结果落库失败 bizNo={}", bizNo, e);
            throw new BusinessException(ArticleErrorCode.TITLE_OPTIMIZE_FAILED);
        }
        return toVo(titles, false);
    }

    private Article mustFind(Long userId, String bizNo) {
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getUserId, userId)
                .eq(Article::getBizNo, bizNo)
                .eq(Article::getIsDeleted, 0));
        if (article == null) {
            throw new BusinessException(ArticleErrorCode.ARTICLE_NOT_FOUND);
        }
        return article;
    }

    private TitleOptimizeVO toVo(Map<String, List<String>> titles, boolean cached) {
        TitleOptimizeVO vo = new TitleOptimizeVO();
        vo.setTitles(titles);
        vo.setCached(cached);
        return vo;
    }

    /** 解析 {"titles":{platform:[...]}}，过滤空串、每平台截断到 2 条；无可平台数据返回 null。 */
    private Map<String, List<String>> parseTitles(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode titlesNode = root.path("titles");
            if (!titlesNode.isObject()) {
                return null;
            }
            Map<String, List<String>> result = new LinkedHashMap<>();
            for (String platform : PLATFORMS) {
                JsonNode arr = titlesNode.path(platform);
                if (!arr.isArray()) {
                    continue;
                }
                List<String> list = new ArrayList<>();
                for (JsonNode item : arr) {
                    String t = item.asText("").trim();
                    if (!t.isEmpty() && list.size() < TITLES_PER_PLATFORM) {
                        list.add(t);
                    }
                }
                if (!list.isEmpty()) {
                    result.put(platform, list);
                }
            }
            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            return null;
        }
    }

    private static String excerpt(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= BODY_EXCERPT_MAX ? body : body.substring(0, BODY_EXCERPT_MAX);
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
