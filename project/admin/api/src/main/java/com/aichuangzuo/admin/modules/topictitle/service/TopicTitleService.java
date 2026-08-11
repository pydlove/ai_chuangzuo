package com.aichuangzuo.admin.modules.topictitle.service;

import com.aichuangzuo.admin.modules.generation.service.AiCallResult;
import com.aichuangzuo.admin.modules.generation.service.GenerationAiService;
import com.aichuangzuo.admin.modules.modelconfig.entity.ModelConfig;
import com.aichuangzuo.admin.modules.modelconfig.mapper.ModelConfigMapper;
import com.aichuangzuo.admin.modules.topictitle.dto.request.TopicTitleQueryRequest;
import com.aichuangzuo.admin.modules.topictitle.entity.TopicTitleTask;
import com.aichuangzuo.admin.modules.topictitle.mapper.TopicTitleMapper;
import com.aichuangzuo.admin.modules.topictitle.mapper.TopicTitleTaskMapper;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理端-标题管理：AI 批量生成标题入库 + 分页查询 + 逻辑删除。
 *
 * <p>生成流程：取当前 active 模型 → 拼装 prompt（方向 + 数量 + JSON 结构 + 强约束）→
 * 同步调 AI → 清洗杂质 → Jackson 解析 → 校验/截断 → 批量入库。
 *
 * <p>异步化：前端调用 {@link #submitTask} 仅入队 t_topic_title_task，
 * {@code TopicTitleTaskWorker} @Scheduled 每 1s 抢一条 QUEUED，
 * 调 {@link #executeTask(Long)} 真正跑 AI；前端按 taskId 轮询状态。
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
    private final TopicTitleTaskMapper topicTitleTaskMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final GenerationAiService generationAiService;
    private final ObjectMapper objectMapper;

    /**
     * 分页列表：标题关键字模糊 + 使用状态筛选（0-未用/1-已用/null-全部）+ 按 id 倒序。
     */
    public TopicTitlePageVO list(TopicTitleQueryRequest req) {
        long page = Math.max(1, req.getPage());
        long pageSize = Math.min(Math.max(1, req.getPageSize()), 100);
        String keyword = req.getKeyword() == null ? "" : req.getKeyword().trim();
        Integer usedStatus = req.getUsedStatus();

        Page<TopicTitle> p = topicTitleMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<TopicTitle>()
                        .like(!keyword.isEmpty(), TopicTitle::getTitle, keyword)
                        .eq(usedStatus != null && usedStatus == 0, TopicTitle::getUseCount, 0)
                        .gt(usedStatus != null && usedStatus == 1, TopicTitle::getUseCount, 0)
                        .orderByDesc(TopicTitle::getId));

        TopicTitlePageVO vo = new TopicTitlePageVO();
        vo.setList(p.getRecords().stream().map(TopicTitleAdminVO::from).toList());
        vo.setTotal(p.getTotal());
        vo.setPage(page);
        vo.setPageSize(pageSize);
        log.info("管理端查询标题列表完成, keyword={}, usedStatus={}, page={}, pageSize={}, total={}",
                keyword, usedStatus, page, pageSize, p.getTotal());
        return vo;
    }

    /**
     * 入队一个新任务，立刻返回 taskId。
     *
     * @throws BusinessException 无 active 模型
     */
    public Long submitTask(int count, String direction) {
        ModelConfig cfg = modelConfigMapper.selectOne(new LambdaQueryWrapper<ModelConfig>()
                .eq(ModelConfig::getIsActive, 1)
                .orderByAsc(ModelConfig::getId)
                .last("LIMIT 1"));
        if (cfg == null) {
            log.warn("AI 生成标题入队失败：无 active 模型配置 count={} direction={}", count, direction);
            throw new BusinessException(AdminGenerationErrorCode.GENERATION_MODEL_UNAVAILABLE);
        }

        TopicTitleTask task = new TopicTitleTask();
        task.setStatus(0);
        task.setCount(count);
        task.setDirection(truncate(direction == null ? "" : direction.trim(), MAX_DIRECTION_LEN));
        task.setGeneratedCount(0);
        topicTitleTaskMapper.insert(task);
        log.info("AI 生成标题入队 taskId={} count={} direction={}",
                task.getId(), count, task.getDirection());
        return task.getId();
    }

    /**
     * Worker 调：执行一个任务。失败抛异常，由 caller 标记 FAILED。
     */
    public void executeTask(Long taskId) {
        TopicTitleTask task = topicTitleTaskMapper.selectById(taskId);
        if (task == null) {
            log.warn("executeTask: task 不存在 id={}", taskId);
            return;
        }
        if (task.getStatus() == null || task.getStatus() != 0) {
            log.warn("executeTask: task 状态非 QUEUED，跳过 id={} status={}", taskId, task.getStatus());
            return;
        }

        task.setStatus(1);
        task.setStartedAt(LocalDateTime.now());
        topicTitleTaskMapper.updateById(task);

        try {
            int generated = runGeneration(task.getCount(), task.getDirection());
            task.setGeneratedCount(generated);
            task.setStatus(2);
            task.setCompletedAt(LocalDateTime.now());
            topicTitleTaskMapper.updateById(task);
            log.info("AI 生成标题完成 taskId={} generated={}", taskId, generated);
        } catch (Exception e) {
            log.error("AI 生成标题失败 taskId={}: {}", taskId, e.getMessage(), e);
            task.setStatus(3);
            task.setFailedReason(truncate(e.getMessage(), 500));
            task.setCompletedAt(LocalDateTime.now());
            topicTitleTaskMapper.updateById(task);
        }
    }

    /**
     * 取任务状态（前端轮询用）。任务不存在抛 NotFoundException。
     */
    public TopicTitleTask getTask(Long taskId) {
        TopicTitleTask t = topicTitleTaskMapper.selectById(taskId);
        if (t == null) {
            throw new NotFoundException("任务不存在 id=" + taskId);
        }
        return t;
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

    /**
     * 实际调 AI + 解析 + 入库。复用原 generate() 的核心逻辑。
     *
     * @return 实际入库条数
     * @throws BusinessException 无 active 模型 / 解析失败
     */
    private int runGeneration(int count, String direction) {
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

    private String buildUserMessage(int count, String direction) {
        String dir = (direction == null || direction.isBlank())
                ? "不限，覆盖职场、情感、生活、AI 等热门自媒体赛道" : direction.trim();
        return "请生成 " + count + " 条自媒体选题标题，每条包含标题和描述（写作指引）。\n\n"
                + "生成方向：" + dir + "\n\n"
                + "支持平台及规则约束（标题与描述必须同时满足）：\n"
                + "- 微信公众号：禁止诱导分享/关注/转发、低俗、谣言、侵权、虚假宣传、标题党。\n"
                + "- 小红书：禁止夸张营销、诱导点赞收藏、虚假体验、违禁词、过度美化/对比。\n"
                + "- 今日头条：禁止标题党、低俗、谣言、侵权、广告法违禁词、无资质医疗/财经建议。\n"
                + "- 知乎：禁止诱导关注、编故事、不友善、低质营销、无来源事实断言。\n"
                + "- 百家号：禁止标题党、低俗、抄袭、广告法违禁词、虚假权威背书。\n"
                + "- 抖音图文：禁止诱导互动（如“双击 666”）、低俗、虚假内容、侵权、未成年人不良引导。\n"
                + "通用禁区：严禁使用“最”“第一”“绝对”“国家级”等无法证实的极限词；严禁制造焦虑、歧视、攻击、泄露隐私；严禁承诺收益、疗效等无法验证的结果。\n\n"
                + "标题多样性要求（避免同质化）：\n"
                + "- 每条标题必须从不同角度切入，避免同义反复或只换关键词。\n"
                + "- 句式要交错使用：问题型、反差型、场景型、观点型、方法型、故事型、数据型等。\n"
                + "- 情绪表达要有差异，避免连续使用“震惊”“绝了”“后悔没早点”等同一套爆款模板。\n"
                + "- 同一生成批次中，任意两条标题的开头 5 个字不能完全相同。\n\n"
                + "描述要求（必须是写作指引，不是简单总结）：\n"
                + "- 说明这篇文章大致怎么写，给出 2-5 个核心观点或写作要点。\n"
                + "- 格式示例：围绕以下观点创作，1、xxx；2、xxxxx；3、xxxx。\n"
                + "- 每个要点要指出：本部分论证什么、从什么角度展开、给读者带来什么价值。\n"
                + "- 不要只写“介绍方法”“分析原因”这类空泛说明。\n\n"
                + "格式要求：\n"
                + "- 标题 ≤30 字，描述 ≤300 字。\n"
                + "- 标题和描述中如需引用词语，一律使用中文双引号“”，不要使用单引号。\n\n"
                + "输出 JSON 结构：\n"
                + "{\"titles\": [{\"title\": \"标题文字\", \"summary\": \"围绕以下观点创作，1、...；2、...；3、...\"}]}\n\n"
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
        return sanitizeJsonStringQuotes(content.substring(start, end + 1));
    }

    /**
     * 修复 AI 在 JSON 字符串值内部使用未转义英文双引号的常见问题。
     *
     * <p>遍历 JSON 时维护字符串状态：当处于字符串内且遇到未转义的 " 时，
     * 如果下一个非空白字符不是结构分隔符（,:}])），则判定为字符串内容里的引号，
     * 前置反斜杠转义，使 Jackson 能正常解析。
     */
    private String sanitizeJsonStringQuotes(String json) {
        StringBuilder sb = new StringBuilder(json.length() + 16);
        boolean inString = false;
        boolean escape = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                sb.append(c);
                escape = false;
                continue;
            }
            if (c == '\\') {
                sb.append(c);
                escape = true;
                continue;
            }
            if (c == '"') {
                if (!inString) {
                    inString = true;
                    sb.append(c);
                } else {
                    int j = i + 1;
                    while (j < json.length() && Character.isWhitespace(json.charAt(j))) {
                        j++;
                    }
                    char next = j < json.length() ? json.charAt(j) : '\0';
                    if (next == ',' || next == ':' || next == '}' || next == ']') {
                        inString = false;
                        sb.append(c);
                    } else {
                        sb.append('\\').append(c);
                    }
                }
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        return s == null ? null : (s.length() > max ? s.substring(0, max) : s);
    }

    /** 日志用截断：AI 原始返回可能很长，最多打 500 字符。 */
    private static String abbreviate(String s) {
        if (s == null) {
            return "<null>";
        }
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
}