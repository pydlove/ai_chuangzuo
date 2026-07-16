package com.aichuangzuo.admin.modules.topictitle.service;

import com.aichuangzuo.admin.modules.generation.service.AiCallResult;
import com.aichuangzuo.admin.modules.generation.service.GenerationAiService;
import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.aichuangzuo.admin.modules.modelconfig.mapper.ModelConfigMapper;
import com.aichuangzuo.admin.modules.topictitle.dto.request.TopicTitleQueryRequest;
import com.aichuangzuo.admin.modules.topictitle.mapper.TopicTitleMapper;
import com.aichuangzuo.admin.modules.topictitle.vo.TopicTitleAdminVO;
import com.aichuangzuo.admin.modules.topictitle.vo.TopicTitlePageVO;
import com.aichuangzuo.shared.entity.TopicTitle;
import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.shared.exception.NotFoundException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理端-标题管理：AI 批量生成标题入库 + 分页查询 + 逻辑删除。
 *
 * <p>生成流程：取当前 active 模型 → 拼装 prompt（方向 + 数量 + JSON 结构 + 强约束）→
 * 同步调 AI → 清洗杂质 → Jackson 解析 → 校验/截断 → 批量入库。
 * 解析失败抛 {@link AdminGenerationErrorCode#TOPIC_TITLE_GENERATE_FAILED}，不入库，管理员可重试。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TopicTitleService {

    /** 固定角色设定。 */
    private static final String SYSTEM_MESSAGE = "你是自媒体爆款标题策划。";

    /** 最终输出强约束（内置后端，原文出自需求，勿改措辞）。 */
    private static final String STRICT_OUTPUT_RULES = """
            最终输出要求（覆盖以上所有说明，必须严格遵守）：
            1. 只输出一个合法 JSON 对象。不要任何前言、说明、免责声明、思路解释、markdown 标题或后记。
            2. 不要用 ```json 或任何代码围栏包裹。
            3. 第一个字符必须是 {，最后一个字符必须是 }。
            4. 所有需要解释、标注、声明的信息，必须放进 JSON 字段里，不能写在 JSON 之外。""";

    private static final int MAX_TITLE_LEN = 128;
    private static final int MAX_SUMMARY_LEN = 512;
    private static final int MAX_DIRECTION_LEN = 1024;

    private final TopicTitleMapper topicTitleMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final GenerationAiService generationAiService;
    private final ObjectMapper objectMapper;

    /**
     * 分页列表：标题关键字模糊 + 按 id 倒序。
     */
    public TopicTitlePageVO list(TopicTitleQueryRequest req) {
        long page = Math.max(1, req.getPage());
        long pageSize = Math.min(Math.max(1, req.getPageSize()), 100);
        String keyword = req.getKeyword() == null ? "" : req.getKeyword().trim();

        Page<TopicTitle> p = topicTitleMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<TopicTitle>()
                        .like(!keyword.isEmpty(), TopicTitle::getTitle, keyword)
                        .orderByDesc(TopicTitle::getId));

        TopicTitlePageVO vo = new TopicTitlePageVO();
        vo.setList(p.getRecords().stream().map(TopicTitleAdminVO::from).toList());
        vo.setTotal(p.getTotal());
        vo.setPage(page);
        vo.setPageSize(pageSize);
        return vo;
    }

    /**
     * AI 生成标题并入库，返回实际入库条数。
     *
     * @throws BusinessException 无 active 模型（GENERATION_MODEL_UNAVAILABLE）/ 解析失败（TOPIC_TITLE_GENERATE_FAILED）
     */
    public int generate(int count, String direction) {
        ModelConfig cfg = modelConfigMapper.selectOne(new LambdaQueryWrapper<ModelConfig>()
                .eq(ModelConfig::getIsActive, 1)
                .orderByAsc(ModelConfig::getId)
                .last("LIMIT 1"));
        if (cfg == null) {
            log.warn("AI 生成标题失败：无 active 模型配置 count={} direction={}", count, direction);
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_MODEL_UNAVAILABLE);
        }
        log.info("AI 生成标题开始 count={} direction={} modelId={} provider={} modelCode={}",
                count, direction, cfg.getId(), cfg.getProviderType(), cfg.getModelCode());

        long startMs = System.currentTimeMillis();
        AiCallResult result = generationAiService.call(cfg.getId(), SYSTEM_MESSAGE,
                buildUserMessage(count, direction), null);
        log.info("AI 生成标题调用返回 duration={}ms contentLength={} tokens={}",
                System.currentTimeMillis() - startMs,
                result.getContent() == null ? 0 : result.getContent().length(),
                result.getTotalTokens());

        List<TopicTitle> titles = parseTitles(result.getContent(), direction);
        titles.forEach(topicTitleMapper::insert);
        log.info("AI 生成标题入库 {} 条（请求 {} 条）direction={}", titles.size(), count, direction);
        return titles.size();
    }

    /**
     * 逻辑删除：已被使用记录引用的标题不能物理删除（破坏「我的已用」排除逻辑）。
     *
     * <p>先加载再删：deleteById(id) 会用空实体把 updated_by 更新为 null，
     * 触发 NOT NULL 约束；传完整实体则沿用库内值。
     */
    public void delete(Long id) {
        TopicTitle title = topicTitleMapper.selectById(id);
        if (title == null) {
            throw new NotFoundException("标题不存在");
        }
        topicTitleMapper.deleteById(title);
    }

    private String buildUserMessage(int count, String direction) {
        String dir = (direction == null || direction.isBlank())
                ? "不限，覆盖职场、情感、生活、AI 等热门自媒体赛道" : direction.trim();
        return "请生成 " + count + " 条自媒体爆款标题，每条包含标题和概要（写作方向）。\n\n"
                + "生成方向：" + dir + "\n\n"
                + "格式要求：标题和概要中如需引用词语，一律使用中文双引号“”，不要使用单引号。\n\n"
                + "输出 JSON 结构：\n"
                + "{\"titles\": [{\"title\": \"标题文字\", \"summary\": \"这篇文章的核心观点和写作方向\"}]}\n\n"
                + STRICT_OUTPUT_RULES;
    }

    /**
     * 解析 AI 输出为标题实体列表：清洗可能的前言/代码围栏 → Jackson 解析 →
     * 剔除 title/summary 为空的条目 → 截断超长。无任何有效条目时抛业务异常。
     */
    private List<TopicTitle> parseTitles(String content, String direction) {
        JsonNode root;
        try {
            root = objectMapper.readTree(extractJson(content));
        } catch (BusinessException e) {
            log.warn("AI 生成标题解析失败：无法定位 JSON 内容，AI 原始返回（截断 500 字符）：{}", abbreviate(content));
            throw e;
        } catch (Exception e) {
            log.warn("AI 生成标题解析失败：JSON 格式错误 err={}，AI 原始返回（截断 500 字符）：{}",
                    e.getMessage(), abbreviate(content));
            throw new BusinessException(AdminGenerationErrorCode.TOPIC_TITLE_GENERATE_FAILED);
        }
        JsonNode titlesNode = root.path("titles");
        if (!titlesNode.isArray() || titlesNode.isEmpty()) {
            log.warn("AI 生成标题解析失败：titles 数组缺失或为空，AI 原始返回（截断 500 字符）：{}", abbreviate(content));
            throw new BusinessException(AdminGenerationErrorCode.TOPIC_TITLE_GENERATE_FAILED);
        }

        List<TopicTitle> result = new ArrayList<>();
        int skipped = 0;
        for (JsonNode node : titlesNode) {
            String title = node.path("title").asText("").trim();
            String summary = node.path("summary").asText("").trim();
            if (title.isEmpty() || summary.isEmpty()) {
                skipped++;
                continue;
            }
            TopicTitle entity = new TopicTitle();
            entity.setTitle(truncate(title, MAX_TITLE_LEN));
            entity.setSummary(truncate(summary, MAX_SUMMARY_LEN));
            entity.setDirection(truncate(direction == null ? "" : direction.trim(), MAX_DIRECTION_LEN));
            entity.setUseCount(0);
            entity.setTenantId(0L);
            result.add(entity);
        }
        if (result.isEmpty()) {
            log.warn("AI 生成标题解析失败：{} 条候选全部缺少 title/summary，AI 原始返回（截断 500 字符）：{}",
                    titlesNode.size(), abbreviate(content));
            throw new BusinessException(AdminGenerationErrorCode.TOPIC_TITLE_GENERATE_FAILED);
        }
        log.info("AI 生成标题解析完成：有效 {} 条，跳过空条目 {} 条", result.size(), skipped);
        return result;
    }

    /** 截取第一个 { 到最后一个 } 之间的内容，剥掉 AI 可能输出的前言/后记/代码围栏。 */
    private String extractJson(String content) {
        if (content == null) {
            throw new BusinessException(AdminGenerationErrorCode.TOPIC_TITLE_GENERATE_FAILED);
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException(AdminGenerationErrorCode.TOPIC_TITLE_GENERATE_FAILED);
        }
        return content.substring(start, end + 1);
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    /** 日志用截断：AI 原始返回可能很长，最多打 500 字符。 */
    private static String abbreviate(String s) {
        if (s == null) {
            return "<null>";
        }
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
}
