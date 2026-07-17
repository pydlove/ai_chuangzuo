package com.aichuangzuo.user.modules.article.service.impl;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.article.entity.Article;
import com.aichuangzuo.user.modules.article.enums.ArticleErrorCode;
import com.aichuangzuo.user.modules.article.mapper.ArticleMapper;
import com.aichuangzuo.user.modules.article.service.TitleOptimizeAiService;
import com.aichuangzuo.user.modules.article.service.TitleOptimizeService;
import com.aichuangzuo.user.modules.article.vo.TitleOptimizeVO;
import com.aichuangzuo.user.modules.benefit.enums.BenefitErrorCode;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
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

    private static final String SYSTEM_MESSAGE =
            "你是一位资深新媒体标题策划专家，深谙各内容平台的推荐机制与用户点击心理。你只输出合法 JSON。";

    /** 用户消息模板：%s 依次为文章标题、正文摘要。 */
    private static final String USER_PROMPT_TEMPLATE = """
            请根据文章标题和正文，为 7 个平台分别拟定 2 条优化标题。

            【文章标题】
            %s

            【文章正文】
            %s

            【平台与风格要求】
            - wechat（公众号）：引发共鸣或好奇，可带数字/悬念，避免标题党词汇堆砌，30 字以内。
            - xiaohongshu（小红书）：口语化、带 emoji，突出获得感或身份代入，20 字以内。
            - toutiao（今日头条）：信息量大、冲击力强，可适度悬念，30 字以内。
            - baijiahao（百家号）：正式稳重、突出价值点与专业性，30 字以内。
            - zhihu（知乎）：以问句或深度观点句呈现，强调逻辑与干货，35 字以内。
            - douyin（抖音图文）：短平快、情绪强、钩子前置，20 字以内。
            - bilibili（B站专栏）：年轻化、有梗但不低俗，突出兴趣点，30 字以内。

            【硬性要求】
            1. 每个平台恰好 2 条标题，风格不可雷同：一条偏痛点/利益驱动，一条偏好奇/情绪驱动。
            2. 标题必须忠于正文内容，不得虚构正文不存在的事实、数据或承诺。
            3. 不得使用“震惊”“不看后悔”等低俗标题党词汇。
            4. 输出 JSON 结构：{"titles":{"wechat":["...","..."],"xiaohongshu":["...","..."],"toutiao":["...","..."],"baijiahao":["...","..."],"zhihu":["...","..."],"douyin":["...","..."],"bilibili":["...","..."]}}

            最终输出要求（覆盖以上所有说明，必须严格遵守）：
              1. 只输出一个合法 JSON 对象。不要任何前言、说明、免责声明、思路解释、markdown 标题或后记。
              2. 不要用 ```json 或任何代码围栏包裹。
              3. 第一个字符必须是 {，最后一个字符必须是 }。
              4. 所有需要解释、标注、声明的信息，必须放进 JSON 字段里，不能写在 JSON 之外。
            """;

    private final ArticleMapper articleMapper;
    private final BenefitService benefitService;
    private final TitleOptimizeAiService aiService;
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

        String userPrompt = String.format(USER_PROMPT_TEMPLATE, article.getTitle(), excerpt(article.getBody()));
        String aiResp = aiService.call(SYSTEM_MESSAGE, userPrompt);
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
