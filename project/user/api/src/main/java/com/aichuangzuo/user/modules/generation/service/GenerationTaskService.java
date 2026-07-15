package com.aichuangzuo.user.modules.generation.service;

import com.aichuangzuo.shared.entity.GenerationTask;
import com.aichuangzuo.shared.entity.PromptTemplate;
import com.aichuangzuo.shared.enums.GenerationTaskStatus;
import com.aichuangzuo.shared.enums.error.UserGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.infrastructure.security.SecurityUserContext;
import com.aichuangzuo.user.modules.generation.dto.request.GenerationRetryRequest;
import com.aichuangzuo.user.modules.generation.dto.request.GenerationSubmitRequest;
import com.aichuangzuo.user.modules.generation.mapper.GenerationActiveModelConfigMapper;
import com.aichuangzuo.user.modules.generation.mapper.GenerationTaskMapper;
import com.aichuangzuo.user.modules.generation.mapper.UserPromptTemplateMapper;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.generation.vo.GenerationTaskPageVO;
import com.aichuangzuo.user.modules.generation.vo.GenerationTaskVO;
import com.aichuangzuo.user.modules.style.entity.UserStyle;
import com.aichuangzuo.user.modules.style.mapper.UserStyleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 用户端-创作任务服务：提交 / 查进度 / 重试 / 列表。
 *
 * <p>提交流程：限流 → 扣 1 次 AI 文章额度（ai_article_quota）→ 入队（status=queued）。
 * 失败由 admin worker 调内部接口退额度。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerationTaskService {

    /** 文章生成对应的权益编码。 */
    private static final String ARTICLE_QUOTA_BENEFIT = "ai_article_quota";

    private final GenerationTaskMapper taskMapper;
    private final GenerationActiveModelConfigMapper activeModelConfigMapper;
    private final UserPromptTemplateMapper promptTemplateMapper;
    private final GenerationBenefitResolver benefitResolver;
    private final GenerationRateLimiter rateLimiter;
    private final BenefitService benefitService;
    private final UserStyleMapper userStyleMapper;
    private final ObjectMapper objectMapper;

    /**
     * 用户提交创作任务。
     */
    @Transactional(rollbackFor = Exception.class)
    public GenerationTaskVO submit(GenerationSubmitRequest req, Long userId) {
        // 1. 限流
        rateLimiter.check(userId, benefitResolver.ratePerMinute(userId));

        // 2. 选模型
        Long modelConfigId = req.getModelConfigId();
        if (modelConfigId == null) {
            modelConfigId = activeModelConfigMapper.selectActiveId();
            if (modelConfigId == null) {
                throw new BusinessException(UserGenerationErrorCode.GENERATION_MODEL_UNAVAILABLE);
            }
        }

        // 3. 锁定唯一已发布模板（task 锁定版本）
        List<PromptTemplate> published = promptTemplateMapper.selectPublished();
        if (published.isEmpty()) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_TEMPLATE_DISABLED);
        }
        PromptTemplate template = published.get(0);
        Integer lockedVersion = template.getLatestPublishedVersion();
        if (lockedVersion == null) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_TEMPLATE_DISABLED);
        }

        // 4. 扣 1 次文章额度 + 入队
        Integer retentionDays = benefitResolver.retentionDays(userId);
        String inputParam = buildInputParam(userId, req);
        String bizNo = generateBizNo();
        benefitService.consume(userId, ARTICLE_QUOTA_BENEFIT);

        GenerationTask task = new GenerationTask();
        task.setBizNo(bizNo);
        task.setTargetUserId(userId);
        task.setStatus(GenerationTaskStatus.QUEUED);
        task.setModelConfigId(modelConfigId);
        task.setPromptTemplateId(template.getId());
        task.setPromptTemplateVersion(lockedVersion);
        task.setInputParam(inputParam);
        task.setWordLimitTarget(req.getWordCount());
        task.setRetryCount(0);
        task.setRetentionDays(retentionDays);
        task.setTenantId(0L);
        task.setIsDeleted(0);
        task.setCreatedBy(userId);
        task.setUpdatedBy(userId);
        taskMapper.insert(task);

        log.info("user={} 提交生成 task={} bizNo={} templateId={} version={} wordCount={}",
                userId, task.getId(), bizNo, template.getId(), lockedVersion, req.getWordCount());
        return GenerationTaskVO.from(task, objectMapper);
    }

    /** 查进度（限本人）。 */
    public GenerationTaskVO getProgress(Long taskId, Long userId) {
        GenerationTask task = requireOwnedTask(taskId, userId);
        return GenerationTaskVO.from(task, objectMapper);
    }

    /**
     * 用户点「重新生成」：写一条新 task，再扣 1 次文章额度。源任务（失败/已完成）保留。
     */
    @Transactional(rollbackFor = Exception.class)
    public GenerationTaskVO retry(Long sourceTaskId, GenerationRetryRequest req, Long userId) {
        GenerationTask source = requireOwnedTask(sourceTaskId, userId);
        if (source.getStatus() != GenerationTaskStatus.FAILED
                && source.getStatus() != GenerationTaskStatus.COMPLETED) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_TASK_NOT_FOUND);
        }

        // 限流
        rateLimiter.check(userId, benefitResolver.ratePerMinute(userId));

        // 扣 1 次文章额度
        String bizNo = generateBizNo();
        benefitService.consume(userId, ARTICLE_QUOTA_BENEFIT);

        // 新 task：沿用 source 输入参数，可选覆盖 wordCount
        Map<String, Object> input = parseInput(source.getInputParam());
        if (req != null && req.getSourceTaskId() != null) {
            // 显式指定 sourceTaskId 时只校验所有权，沿用输入
        }
        String inputParam = objectMapper.valueToTree(input).toString();

        GenerationTask task = new GenerationTask();
        task.setBizNo(bizNo);
        task.setTargetUserId(userId);
        task.setStatus(GenerationTaskStatus.QUEUED);
        task.setModelConfigId(source.getModelConfigId());
        task.setPromptTemplateId(0L);
        task.setInputParam(inputParam);
        task.setWordLimitTarget(source.getWordLimitTarget());
        task.setRetryCount(0);
        task.setRetentionDays(source.getRetentionDays());
        task.setTenantId(0L);
        task.setIsDeleted(0);
        task.setCreatedBy(userId);
        task.setUpdatedBy(userId);
        taskMapper.insert(task);
        log.info("user={} 重新生成 srcTask={} newTask={}", userId, sourceTaskId, task.getId());
        return GenerationTaskVO.from(task, objectMapper);
    }

    /** 我提交过的任务列表（FIFO 反序：最新在前）。 */
    public GenerationTaskPageVO listMine(Long userId, long page, long pageSize) {
        long offset = (page - 1) * pageSize;
        List<GenerationTask> rows = taskMapper.selectUserTasks(userId, List.of(), offset, (int) pageSize);
        long total = taskMapper.countUserTasks(userId, List.of());
        GenerationTaskPageVO vo = new GenerationTaskPageVO();
        vo.setList(rows.stream().map(t -> GenerationTaskVO.from(t, objectMapper)).toList());
        vo.setTotal(total);
        vo.setPage(page);
        vo.setPageSize(pageSize);
        return vo;
    }

    // ---------- helpers ----------

    private GenerationTask requireOwnedTask(Long taskId, Long userId) {
        GenerationTask task = taskMapper.selectById(taskId);
        if (task == null || !userId.equals(task.getTargetUserId())) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_TASK_NOT_FOUND);
        }
        return task;
    }

    private String buildInputParam(Long userId, GenerationSubmitRequest req) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", req.getTitle());
        map.put("description", req.getDescription());
        map.put("platform", req.getPlatform());
        map.put("styleRef", req.getStyleRef());
        map.put("wordCount", req.getWordCount());
        map.put("template", req.getTemplate());
        map.put("toneTags", defaultToneTags(req.getPlatform()));
        // 快照用户风格 prompt：worker 端无需跨表查 u_user_style
        map.put("userStylePrompt", resolveUserStylePrompt(userId, req.getStyleRef()));
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalStateException("serialize input_param failed", e);
        }
    }

    /**
     * 按 (userId, styleName) 查 u_user_style.prompt。
     *
     * <p>找不到（用户填了不存在的风格名、风格被逻辑删除、styleRef 为空）时返回 ""，
     * 不影响任务继续。
     */
    private String resolveUserStylePrompt(Long userId, String styleRef) {
        if (styleRef == null || styleRef.isBlank()) return "";
        LambdaQueryWrapper<UserStyle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserStyle::getUserId, userId)
                .eq(UserStyle::getStyleName, styleRef)
                .last("LIMIT 1");
        UserStyle style = userStyleMapper.selectOne(wrapper);
        return style == null ? "" : nullToEmpty(style.getPrompt());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static List<String> defaultToneTags(String platform) {
        if (platform == null) return List.of();
        return switch (platform.toLowerCase()) {
            case "wechat", "toutiao", "baijiahao", "zhihu" -> List.of("正式", "信息密度高");
            case "xiaohongshu", "douyin" -> List.of("口语化", "emoji 节奏");
            default -> List.of("中性");
        };
    }

    private Map<String, Object> parseInput(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String generateBizNo() {
        return "GA" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
    }
}
