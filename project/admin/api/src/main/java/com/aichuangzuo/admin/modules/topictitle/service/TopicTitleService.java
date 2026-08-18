package com.aichuangzuo.admin.modules.topictitle.service;

import com.aichuangzuo.admin.modules.aiprompt.service.AiPromptRenderService;
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
import com.aichuangzuo.shared.vo.AiPromptRendered;
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
import java.util.Map;

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

    private static final int MAX_TITLE_LEN = 128;
    private static final int MAX_SUMMARY_LEN = 512;
    private static final int MAX_DIRECTION_LEN = 1024;

    private final TopicTitleMapper topicTitleMapper;
    private final TopicTitleTaskMapper topicTitleTaskMapper;
    private final ModelConfigMapper modelConfigMapper;
    private final GenerationAiService generationAiService;
    private final AiPromptRenderService aiPromptRenderService;
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
     * 批量逻辑删除。
     *
     * <p>只处理仍然存在的记录；不存在的 ID 静默跳过（前端批量勾选后列表刷新，
     * 部分记录可能已被其它管理员删除）。返回实际删除条数。
     */
    public int deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<TopicTitle> titles = topicTitleMapper.selectBatchIds(ids);
        if (titles == null || titles.isEmpty()) {
            return 0;
        }
        for (TopicTitle title : titles) {
            topicTitleMapper.deleteById(title);
        }
        log.info("管理端批量删除标题完成, ids={}, deleted={}", ids, titles.size());
        return titles.size();
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
        String directionText = (direction == null || direction.isBlank())
                ? "不限，覆盖职场、情感、生活、AI 等热门自媒体赛道" : direction.trim();
        AiPromptRendered prompt = aiPromptRenderService.render("topic_title_v1",
                Map.of("count", count, "direction", directionText));
        AiCallResult result = generationAiService.call(cfg.getId(), prompt.systemRole(),
                prompt.userPrompt(), null);
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