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
import com.aichuangzuo.user.modules.skill.entity.UserSkill;
import com.aichuangzuo.user.modules.skill.mapper.UserSkillMapper;
import com.aichuangzuo.user.modules.skill.market.entity.SkillMarket;
import com.aichuangzuo.user.modules.skill.market.mapper.SkillMarketMapper;
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

    /** 用户手动停止时的失败原因（前端据此显示「已停止」）。 */
    private static final String USER_STOP_REASON = "用户手动停止";

    private final GenerationTaskMapper taskMapper;
    private final GenerationActiveModelConfigMapper activeModelConfigMapper;
    private final UserPromptTemplateMapper promptTemplateMapper;
    private final GenerationBenefitResolver benefitResolver;
    private final GenerationRateLimiter rateLimiter;
    private final BenefitService benefitService;
    private final UserSkillMapper userSkillMapper;
    private final SkillMarketMapper skillMarketMapper;
    private final ObjectMapper objectMapper;

    /**
     * 用户提交创作任务。
     */
    @Transactional(rollbackFor = Exception.class)
    public GenerationTaskVO submit(GenerationSubmitRequest req, Long userId) {
        // 1. 校验当前套餐队列任务数上限（避免基础版等低配套餐同时堆积多个任务）
        checkQueueLimit(userId);

        // 2. 限流
        rateLimiter.check(userId, benefitResolver.ratePerMinute(userId));

        // 3. 校验当前套餐字数上限
        int wordLimit = parseInt(benefitService.getPlanBenefitValue(userId, "generation_word_limit", "500"), 500);
        if (req.getWordCount() != null && req.getWordCount() > wordLimit) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_WORD_LIMIT_EXCEEDS_PLAN);
        }

        // 3.5 校验所选提示词是否可用（尤其是提示词市场中的 skill）
        validateSkillRef(req.getSkillRef());

        // 4. 选模型
        Long modelConfigId = req.getModelConfigId();
        if (modelConfigId == null) {
            modelConfigId = activeModelConfigMapper.selectActiveId();
            if (modelConfigId == null) {
                throw new BusinessException(UserGenerationErrorCode.GENERATION_MODEL_UNAVAILABLE);
            }
        }

        // 5. 锁定唯一已发布模板（task 锁定版本）
        List<PromptTemplate> published = promptTemplateMapper.selectPublished();
        if (published.isEmpty()) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_TEMPLATE_DISABLED);
        }
        PromptTemplate template = published.get(0);
        Integer lockedVersion = template.getLatestPublishedVersion();
        if (lockedVersion == null) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_TEMPLATE_DISABLED);
        }

        // 6. 扣 1 次文章额度 + 入队
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

        // 校验当前套餐队列任务数上限
        checkQueueLimit(userId);

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

    /** 用户手动停止任务：QUEUED / PROCESSING → FAILED，退回 1 次文章额度。
     *
     * <p>PROCESSING 任务会被 worker 在下一 stage 前协作式中止（已置 FAILED + 清 lease）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void stop(Long taskId, Long userId) {
        GenerationTask task = requireOwnedTask(taskId, userId);
        if (task.getStatus() != GenerationTaskStatus.QUEUED
                && task.getStatus() != GenerationTaskStatus.PROCESSING) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_TASK_INVALID_STATUS);
        }

        task.setStatus(GenerationTaskStatus.FAILED);
        task.setFailedReason(USER_STOP_REASON);
        task.setCompletedAt(LocalDateTime.now());
        task.setLockedAt(null);
        task.setLockedBy(null);
        task.setLeaseUntil(null);
        taskMapper.updateById(task);
        log.info("user={} 手动停止任务 task={}", userId, taskId);

        try {
            benefitService.refund(userId, ARTICLE_QUOTA_BENEFIT);
        } catch (Exception e) {
            log.error("task={} 手动停止后退文章额度失败，需人工介入: {}", taskId, e.getMessage());
        }
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

    /** 校验当前队列任务数是否超过套餐上限（QUEUED / PROCESSING 均占坑）。 */
    private void checkQueueLimit(Long userId) {
        int maxQueueTasks = parseInt(benefitService.getPlanBenefitValue(userId, "queue_max_tasks", "1"), 1);
        long activeTaskCount = taskMapper.countUserTasks(userId,
                List.of(GenerationTaskStatus.QUEUED.getCode(), GenerationTaskStatus.PROCESSING.getCode()));
        if (activeTaskCount >= maxQueueTasks) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_QUEUE_LIMIT_EXCEEDED);
        }
    }

    private String buildInputParam(Long userId, GenerationSubmitRequest req) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", req.getTitle());
        map.put("description", req.getDescription());
        map.put("platform", req.getPlatform());
        map.put("skillRef", req.getSkillRef());
        map.put("wordCount", req.getWordCount());
        map.put("template", req.getTemplate());
        map.put("toneTags", defaultToneTags(req.getPlatform()));
        // 快照 SEO 关键词建议权益：admin worker 生成推荐标签时按此过滤
        map.put("seoKeywords", benefitService.getPlanBenefitValue(userId, "seo_keywords", "false"));
        // 快照用户风格 prompt：worker 端无需跨表查 u_user_skill
        map.put("userSkillPrompt", resolveUserSkillPrompt(userId, req.getSkillRef()));
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalStateException("serialize input_param failed", e);
        }
    }

    /**
     * 按 skillRef 解析用户风格 prompt。
     *
     * <p>匹配顺序：
     * <ol>
     *   <li>当前用户的自定义/学习风格（skillName 匹配）</li>
     *   <li>系统预设风格（source_type=3、enable_status=1、skillName 匹配）</li>
     *   <li>提示词市场风格（bizNo 匹配，用于从市场选择 skill 的场景）</li>
     * </ol>
     * 都找不到（或 skillRef 为空）时返回 ""，不影响任务继续。
     */
    private String resolveUserSkillPrompt(Long userId, String skillRef) {
        if (skillRef == null || skillRef.isBlank()) return "";

        // 1. 优先匹配用户自己的风格
        LambdaQueryWrapper<UserSkill> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getSkillName, skillRef)
                .eq(UserSkill::getIsDeleted, 0)
                .last("LIMIT 1");
        UserSkill userSkill = userSkillMapper.selectOne(userWrapper);
        if (userSkill != null) {
            return nullToEmpty(userSkill.getPrompt());
        }

        // 2. 再匹配系统预设风格（source_type=3，启用中）
        LambdaQueryWrapper<UserSkill> systemWrapper = new LambdaQueryWrapper<>();
        systemWrapper.eq(UserSkill::getSourceType, 3)
                .eq(UserSkill::getEnableStatus, 1)
                .eq(UserSkill::getIsDeleted, 0)
                .eq(UserSkill::getSkillName, skillRef)
                .last("LIMIT 1");
        UserSkill systemSkill = userSkillMapper.selectOne(systemWrapper);
        if (systemSkill != null) {
            return nullToEmpty(systemSkill.getPrompt());
        }

        // 3. 提示词市场风格（skillRef 为市场 skill 的 bizNo）
        LambdaQueryWrapper<SkillMarket> marketWrapper = new LambdaQueryWrapper<>();
        marketWrapper.eq(SkillMarket::getBizNo, skillRef)
                .eq(SkillMarket::getEnableStatus, 1)
                .eq(SkillMarket::getAuditStatus, 1)
                .eq(SkillMarket::getIsDeleted, 0)
                .last("LIMIT 1");
        SkillMarket marketSkill = skillMarketMapper.selectOne(marketWrapper);
        return marketSkill == null ? "" : nullToEmpty(marketSkill.getPrompt());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * 校验 skillRef 是否指向一个已下架/不可用的提示词市场 skill。
     *
     * <p>如果是市场 skill（bizNo 匹配）但已删除、未启用或未审核通过，则直接阻断。
     * 非市场 skill（用户自定义/系统预设）不做强校验，按原有逻辑走。
     */
    private void validateSkillRef(String skillRef) {
        if (skillRef == null || skillRef.isBlank()) {
            return;
        }
        LambdaQueryWrapper<SkillMarket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillMarket::getBizNo, skillRef).last("LIMIT 1");
        SkillMarket marketSkill = skillMarketMapper.selectOne(wrapper);
        if (marketSkill == null) {
            return;
        }
        if (marketSkill.getIsDeleted() != null && marketSkill.getIsDeleted() == 1) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_SKILL_NOT_AVAILABLE);
        }
        if (marketSkill.getEnableStatus() == null || marketSkill.getEnableStatus() != 1) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_SKILL_NOT_AVAILABLE);
        }
        if (marketSkill.getAuditStatus() == null || marketSkill.getAuditStatus() != 1) {
            throw new BusinessException(UserGenerationErrorCode.GENERATION_SKILL_NOT_AVAILABLE);
        }
    }

    private static List<String> defaultToneTags(String platform) {
        if (platform == null) return List.of();
        return switch (platform.toLowerCase()) {
            case "wechat", "toutiao", "baijiahao", "zhihu", "bilibili" -> List.of("正式", "信息密度高");
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

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
