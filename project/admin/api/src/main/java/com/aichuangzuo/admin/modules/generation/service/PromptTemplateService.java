package com.aichuangzuo.admin.modules.generation.service;

import com.aichuangzuo.admin.modules.generation.dto.request.CloneTemplateRequest;
import com.aichuangzuo.admin.modules.generation.dto.request.PromptTemplateListRequest;
import com.aichuangzuo.admin.modules.generation.dto.request.PromptTemplateSaveRequest;
import com.aichuangzuo.admin.modules.generation.dto.request.PromptTemplateStageSaveItem;
import com.aichuangzuo.admin.modules.generation.entity.PromptTemplateStage;
import com.aichuangzuo.admin.modules.generation.mapper.PromptTemplateMapper;
import com.aichuangzuo.admin.modules.generation.mapper.PromptTemplateStageMapper;
import com.aichuangzuo.admin.modules.generation.mapper.PromptTemplateVersionMapper;
import com.aichuangzuo.admin.modules.generation.pipeline.ConfigField;
import com.aichuangzuo.admin.modules.generation.pipeline.Placeholder;
import com.aichuangzuo.admin.modules.generation.pipeline.PipelineStage;
import com.aichuangzuo.admin.modules.generation.pipeline.PromptTemplateStageValidator;
import com.aichuangzuo.admin.modules.generation.pipeline.StageType;
import com.aichuangzuo.admin.modules.generation.vo.PromptTemplateAdminPageVO;
import com.aichuangzuo.admin.modules.generation.vo.PromptTemplateAdminVO;
import com.aichuangzuo.admin.modules.generation.vo.PromptTemplateStageVO;
import com.aichuangzuo.admin.modules.generation.vo.PromptTemplateVersionVO;
import com.aichuangzuo.shared.creative.CreativeTemplateConstants;
import com.aichuangzuo.shared.creative.TemplateStatus;
import com.aichuangzuo.shared.entity.PromptTemplate;
import com.aichuangzuo.shared.entity.PromptTemplateVersion;
import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Admin 端-提示词模板服务：CRUD + 启用 / 停用 + 12 阶段配置管理。
 *
 * <p>启用策略：runtime 仅允许 1 个 enabled=1，由事务保证。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private final PromptTemplateMapper mapper;
    private final PromptTemplateStageMapper stageMapper;
    private final PromptTemplateVersionMapper versionMapper;

    public Optional<PromptTemplate> findEnabled() {
        List<PromptTemplate> list = mapper.selectEnabled();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public PromptTemplateAdminPageVO list(PromptTemplateListRequest req) {
        Page<PromptTemplate> page = new Page<>(req.getPage(), req.getPageSize());
        String kw = req.getKeyword() == null ? null : req.getKeyword().trim();
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PromptTemplate>();
        if (kw != null && !kw.isBlank()) {
            wrapper.like(PromptTemplate::getName, kw);
        }
        wrapper.orderByDesc(PromptTemplate::getId);
        Page<PromptTemplate> result = mapper.selectPage(page, wrapper);
        PromptTemplateAdminPageVO vo = new PromptTemplateAdminPageVO();
        vo.setList(result.getRecords().stream().map(this::toVo).toList());
        vo.setTotal(result.getTotal());
        vo.setPage(result.getCurrent());
        vo.setPageSize(result.getSize());
        return vo;
    }

    public PromptTemplateAdminVO detail(Long id) {
        PromptTemplate t = requireById(id);
        return toVo(t);
    }

    @Transactional
    public Long create(PromptTemplateSaveRequest req, Long adminUserId) {
        PromptTemplate t = new PromptTemplate();
        BeanUtils.copyProperties(req, t, "id", "stages");
        t.setEnabled(0);
        t.setTemplateStatus(TemplateStatus.DRAFT.code);
        t.setLatestPublishedVersion(null);
        t.setTenantId(0L);
        t.setIsDeleted(0);
        t.setCreatedBy(adminUserId == null ? 0L : adminUserId);
        t.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
        mapper.insert(t);

        // 自动建 12 阶段（按用户传值；空则用 PipelineStage 默认）
        List<PromptTemplateStage> stages = buildStages(t.getId(), req.getStages(), adminUserId);
        for (PromptTemplateStage s : stages) {
            stageMapper.insert(s);
        }
        log.info("admin={} 创建 prompt template id={} name={} stages={}",
                adminUserId, t.getId(), t.getName(), stages.size());
        return t.getId();
    }

    @Transactional
    public void update(Long id, PromptTemplateSaveRequest req, Long adminUserId) {
        PromptTemplate exist = requireById(id);
        BeanUtils.copyProperties(req, exist, "id", "enabled", "stages");
        exist.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
        mapper.updateById(exist);

        if (req.getStages() != null && !req.getStages().isEmpty()) {
            // 物理删除旧 stage + 重新插入（全量替换）
            stageMapper.deleteByTemplateId(id);
            List<PromptTemplateStage> stages = buildStages(id, req.getStages(), adminUserId);
            for (PromptTemplateStage s : stages) {
                stageMapper.insert(s);
            }
        }
        log.info("admin={} 更新 prompt template id={} name={}", adminUserId, id, exist.getName());
    }

    /**
     * 老模板初始化 12 阶段默认值（点击「初始化 12 阶段」按钮触发）。
     * 已有 stage 则不动。
     */
    @Transactional
    public int initStages(Long id, Long adminUserId) {
        requireById(id);
        List<PromptTemplateStage> exist = stageMapper.selectByTemplateId(id);
        if (!exist.isEmpty()) {
            log.info("admin={} 模板 id={} 已有 {} 个 stage，跳过初始化", adminUserId, id, exist.size());
            return 0;
        }
        List<PromptTemplateStage> stages = buildStages(id, null, adminUserId);
        for (PromptTemplateStage s : stages) {
            stageMapper.insert(s);
        }
        log.info("admin={} 初始化模板 id={} 的 12 阶段默认值", adminUserId, id);
        return stages.size();
    }

    @Transactional
    public void enable(Long id, Long adminUserId) {
        PromptTemplate exist = requireById(id);
        LambdaUpdateWrapper<PromptTemplate> clearAll = Wrappers.lambdaUpdate(PromptTemplate.class)
                .eq(PromptTemplate::getEnabled, 1)
                .set(PromptTemplate::getEnabled, 0)
                .set(PromptTemplate::getTemplateStatus, TemplateStatus.OFFLINE.code);
        mapper.update(null, clearAll);
        PromptTemplate t = new PromptTemplate();
        t.setId(id);
        t.setEnabled(1);
        t.setTemplateStatus(TemplateStatus.PUBLISHED.code);
        t.setLatestPublishedVersion(exist.getLatestPublishedVersion());
        t.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
        mapper.updateById(t);
        log.info("admin={} 启用 prompt template id={}", adminUserId, id);
    }

    @Transactional
    public void disable(Long id, Long adminUserId) {
        PromptTemplate exist = requireById(id);
        PromptTemplate t = new PromptTemplate();
        t.setId(id);
        t.setEnabled(0);
        t.setTemplateStatus(TemplateStatus.OFFLINE.code);
        t.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
        mapper.updateById(t);
        log.info("admin={} 停用 prompt template id={}", adminUserId, id);
    }

    @Transactional
    public void delete(Long id, Long adminUserId) {
        PromptTemplate t = requireById(id);
        if (t.getId() != null && t.getId() == CreativeTemplateConstants.DEFAULT_TEMPLATE_ID) {
            throw new BusinessException(AdminGenerationErrorCode.PROMPT_TEMPLATE_BUILTIN_IMMUTABLE);
        }
        stageMapper.deleteByTemplateId(id);
        mapper.deleteById(id);
        log.info("admin={} 删除 prompt template id={}", adminUserId, id);
    }

    // ===== 阶段 2：发布 / 下线 / 克隆 / 版本列表 =====

    /**
     * 发布模板：把当前 12 阶段配置快照成下一个版本号，置状态为 PUBLISHED。
     *
     * <p>草稿、已发布、已下线状态都可发布；版本号单调递增。
     */
    @Transactional
    public Long publish(Long id, String changeNote, Long adminUserId) {
        PromptTemplate t = requireById(id);
        int nextVersion = (t.getLatestPublishedVersion() == null ? 0 : t.getLatestPublishedVersion()) + 1;

        // 1. 快照 12 阶段到 version 表
        List<PromptTemplateStage> rows = stageMapper.selectByTemplateId(id);
        String configJson = serializeConfig(rows);

        PromptTemplateVersion v = new PromptTemplateVersion();
        v.setTemplateId(id);
        v.setVersion(nextVersion);
        v.setVersionStatus(TemplateStatus.PUBLISHED.code);
        v.setConfigJson(configJson);
        v.setChangeNote(changeNote);
        v.setPublishedAt(LocalDateTime.now());
        v.setPublishedBy(adminUserId == null ? 0L : adminUserId);
        v.setTenantId(0L);
        v.setIsDeleted(0);
        v.setCreatedBy(adminUserId == null ? 0L : adminUserId);
        v.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
        versionMapper.insert(v);

        // 2. 把已发布状态的老版本标记为 OFFLINE（保留历史）
        List<PromptTemplateVersion> oldPublished = versionMapper.selectByTemplateId(id);
        for (PromptTemplateVersion old : oldPublished) {
            if (old.getVersionStatus() != null
                    && old.getVersionStatus() == TemplateStatus.PUBLISHED.code
                    && !old.getVersion().equals(nextVersion)) {
                old.setVersionStatus(TemplateStatus.OFFLINE.code);
                old.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
                versionMapper.updateById(old);
            }
        }

        // 3. 更新主表
        t.setTemplateStatus(TemplateStatus.PUBLISHED.code);
        t.setEnabled(1);
        t.setLatestPublishedVersion(nextVersion);
        t.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
        mapper.updateById(t);

        log.info("admin={} 发布 prompt template id={} version={}", adminUserId, id, nextVersion);
        return (long) nextVersion;
    }

    /**
     * 下线模板：把状态置为 OFFLINE，enabled=0。
     *
     * <p>仅当前为 PUBLISHED 状态的模板允许下线；草稿不可下线（草稿本就不生效）。
     */
    @Transactional
    public void offline(Long id, Long adminUserId) {
        PromptTemplate t = requireById(id);
        if (t.getTemplateStatus() == null
                || t.getTemplateStatus() != TemplateStatus.PUBLISHED.code) {
            throw new BusinessException(AdminGenerationErrorCode.PROMPT_TEMPLATE_INVALID_STATUS);
        }
        t.setTemplateStatus(TemplateStatus.OFFLINE.code);
        t.setEnabled(0);
        t.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
        mapper.updateById(t);

        // 把当前已发布版本同步置为 OFFLINE（历史归档）
        PromptTemplateVersion latest = versionMapper.selectLatestPublished(id);
        if (latest != null) {
            latest.setVersionStatus(TemplateStatus.OFFLINE.code);
            latest.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
            versionMapper.updateById(latest);
        }

        log.info("admin={} 下线 prompt template id={}", adminUserId, id);
    }

    /**
     * 克隆模板：把源模板的 12 阶段配置复制为一张新草稿。
     *
     * <p>默认从源模板当前的 stage 表复制；如指定 sourceVersion 则从版本快照解析。
     */
    @Transactional
    public Long clone(Long sourceId, CloneTemplateRequest req, Long adminUserId) {
        PromptTemplate src = requireById(sourceId);

        // 1. 插新模板主表
        PromptTemplate copy = new PromptTemplate();
        BeanUtils.copyProperties(src, copy, "id", "enabled", "latestPublishedVersion",
                "createdAt", "updatedAt");
        copy.setName(req.getName());
        if (req.getRemark() != null) {
            copy.setRemark(req.getRemark());
        }
        copy.setTemplateStatus(TemplateStatus.DRAFT.code);
        copy.setEnabled(0);
        copy.setLatestPublishedVersion(null);
        copy.setCreatedBy(adminUserId == null ? 0L : adminUserId);
        copy.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
        mapper.insert(copy);

        // 2. 复制 stage（来自源 stage 表或指定版本的快照）
        List<PromptTemplateStage> srcStages;
        if (req.getSourceVersion() != null) {
            PromptTemplateVersion v = versionMapper.selectByTemplateId(sourceId).stream()
                    .filter(x -> x.getVersion() != null && x.getVersion().equals(req.getSourceVersion()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(AdminGenerationErrorCode.PROMPT_TEMPLATE_NOT_FOUND));
            srcStages = parseStagesFromConfig(v.getConfigJson());
        } else {
            srcStages = stageMapper.selectByTemplateId(sourceId);
        }
        for (PromptTemplateStage srcStage : srcStages) {
            PromptTemplateStage newStage = new PromptTemplateStage();
            BeanUtils.copyProperties(srcStage, newStage, "id", "templateId", "createdAt", "updatedAt");
            newStage.setTemplateId(copy.getId());
            newStage.setCreatedBy(adminUserId == null ? 0L : adminUserId);
            newStage.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
            stageMapper.insert(newStage);
        }
        log.info("admin={} 克隆 prompt template id={} → newId={} stages={}",
                adminUserId, sourceId, copy.getId(), srcStages.size());
        return copy.getId();
    }

    /**
     * 取模板的全部版本快照（按 version 降序），不含 config_json 全文。
     */
    public List<PromptTemplateVersionVO> listVersions(Long id) {
        requireById(id);
        return versionMapper.selectByTemplateId(id).stream().map(v -> {
            PromptTemplateVersionVO vo = new PromptTemplateVersionVO();
            BeanUtils.copyProperties(v, vo);
            TemplateStatus st = TemplateStatus.fromCode(v.getVersionStatus());
            vo.setVersionStatus(st.code);
            vo.setVersionStatusLabel(st.label);
            return vo;
        }).toList();
    }

    /**
     * 序列化 12 阶段行为 config_json。结构对齐设计文档 §5.10。
     *
     * <p>用简单字符串拼接避免引入额外 JSON 库依赖；如有更复杂场景后续可换 Jackson。
     */
    private String serializeConfig(List<PromptTemplateStage> rows) {
        StringBuilder sb = new StringBuilder("{\"stages\":[");
        boolean first = true;
        for (PromptTemplateStage r : rows) {
            if (!first) sb.append(',');
            first = false;
            sb.append("{\"index\":").append(nullSafe(r.getStageIndex()))
              .append(",\"stageKey\":").append(jsonStr(r.getStageKey()))
              .append(",\"stageType\":").append(jsonStr(r.getStageType()))
              .append(",\"aiPrompt\":").append(jsonStrOrNull(r.getAiPrompt()))
              .append(",\"ruleConfig\":").append(jsonStrOrNull(r.getRuleConfig()))
              .append(",\"modelParams\":").append(jsonStrOrNull(r.getModelParams()))
              .append(",\"enabled\":").append(nullSafe(r.getEnabled()))
              .append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String jsonStr(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    private static String jsonStrOrNull(String s) {
        return s == null ? "null" : jsonStr(s);
    }

    private static String nullSafe(Integer v) {
        return v == null ? "null" : v.toString();
    }

    /**
     * 从 config_json 解析回 stage 行。当前实现仅在 {@link #clone} 用到，
     * 解析失败抛 BusinessException，调用方需保证 config_json 是合法 JSON。
     */
    private List<PromptTemplateStage> parseStagesFromConfig(String configJson) {
        // 简化：用 Jackson ObjectMapper 解析 config_json.stages 数组为 List<Map>，
        // 然后映射回 PromptTemplateStage。如果后续要做版本 diff，可换更结构化的 DTO。
        try {
            com.fasterxml.jackson.databind.ObjectMapper m =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = m.readTree(configJson);
            com.fasterxml.jackson.databind.JsonNode stages = root.get("stages");
            if (stages == null || !stages.isArray()) {
                throw new BusinessException(AdminGenerationErrorCode.PROMPT_TEMPLATE_NOT_FOUND);
            }
            List<PromptTemplateStage> result = new ArrayList<>();
            for (com.fasterxml.jackson.databind.JsonNode n : stages) {
                PromptTemplateStage s = new PromptTemplateStage();
                s.setStageIndex(n.has("index") && !n.get("index").isNull() ? n.get("index").asInt() : null);
                s.setStageKey(n.has("stageKey") && !n.get("stageKey").isNull() ? n.get("stageKey").asText() : null);
                s.setStageType(n.has("stageType") && !n.get("stageType").isNull() ? n.get("stageType").asText() : null);
                s.setAiPrompt(n.has("aiPrompt") && !n.get("aiPrompt").isNull() ? n.get("aiPrompt").asText() : null);
                s.setRuleConfig(n.has("ruleConfig") && !n.get("ruleConfig").isNull() ? n.get("ruleConfig").toString() : null);
                s.setModelParams(n.has("modelParams") && !n.get("modelParams").isNull() ? n.get("modelParams").asText() : null);
                s.setEnabled(n.has("enabled") && !n.get("enabled").isNull() ? n.get("enabled").asInt() : 1);
                result.add(s);
            }
            return result;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new BusinessException(AdminGenerationErrorCode.PROMPT_TEMPLATE_NOT_FOUND);
        }
    }

    private PromptTemplate requireById(Long id) {
        PromptTemplate t = mapper.selectById(id);
        if (t == null) {
            throw new BusinessException(AdminGenerationErrorCode.PROMPT_TEMPLATE_NOT_FOUND);
        }
        return t;
    }

    /**
     * 把请求里的 12 个 stage 转成 entity。
     * 某 stage 用户没传值时，从 {@link PipelineStage} 默认值兜底。
     *
     * <p>注：只入库 index 1-12 的真实阶段；{@link PipelineStage#PERSIST_ARTICLE}（index=100）
     * 是 orchestrator 的合成收尾步骤，不是模板配置的一部分，跳过。
     */
    private List<PromptTemplateStage> buildStages(Long templateId,
                                                 List<PromptTemplateStageSaveItem> items,
                                                 Long adminUserId) {
        List<PromptTemplateStage> result = new ArrayList<>();
        for (PipelineStage def : PipelineStage.ALL) {
            if (def == PipelineStage.PERSIST_ARTICLE) continue;
            PromptTemplateStageSaveItem item = items == null ? null
                    : items.stream()
                            .filter(x -> x.getStageIndex() != null && x.getStageIndex() == def.index)
                            .findFirst()
                            .orElse(null);

            PromptTemplateStage s = new PromptTemplateStage();
            s.setTemplateId(templateId);
            s.setStageIndex(def.index);
            s.setStageType(def.type.code);
            s.setStageKey(def.key);
            s.setEnabled(item != null && item.getEnabled() != null ? item.getEnabled() : 1);

            // AI 阶段：校验 + 序列化 modelParams（PASSTHROUGH / RULE_CONFIG 不接受）
            if (item != null && item.getModelParams() != null && !item.getModelParams().isEmpty()) {
                if (def.type != StageType.AI_PROMPT) {
                    throw new com.aichuangzuo.shared.exception.BusinessException(
                            com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode
                                    .GENERATION_MODEL_PARAMS_INVALID);
                }
                PromptTemplateStageValidator.validate(item.getModelParams());
                s.setModelParams(toJson(item.getModelParams()));
            }

            switch (def.type) {
                case AI_PROMPT:
                    s.setAiPrompt(item != null && item.getAiPrompt() != null
                            ? item.getAiPrompt()
                            : def.defaultAiPrompt);
                    break;
                case RULE_CONFIG:
                    s.setRuleConfig(item != null && item.getRuleConfig() != null
                            ? item.getRuleConfig()
                            : def.defaultRuleConfigJson);
                    break;
                case PASSTHROUGH:
                default:
                    // 无需 aiPrompt / ruleConfig
                    break;
            }
            s.setTenantId(0L);
            s.setIsDeleted(0);
            s.setCreatedBy(adminUserId == null ? 0L : adminUserId);
            s.setUpdatedBy(adminUserId == null ? 0L : adminUserId);
            result.add(s);
        }
        return result;
    }

    private static String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new com.aichuangzuo.shared.exception.BusinessException(
                    com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode
                            .GENERATION_MODEL_PARAMS_INVALID);
        }
    }

    private static java.util.Map<String, Object> parseModelParamsJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            log.warn("modelParams JSON 反序列化失败，前端按 null 显示: {}", e.getMessage());
            return null;
        }
    }

    private PromptTemplateAdminVO toVo(PromptTemplate t) {
        PromptTemplateAdminVO vo = new PromptTemplateAdminVO();
        BeanUtils.copyProperties(t, vo);
        List<PromptTemplateStage> rows = stageMapper.selectByTemplateId(t.getId());
        if (rows == null || rows.isEmpty()) {
            vo.setStages(new ArrayList<>());
            vo.setStagesInitialized(false);
        } else {
            // 补全 stageIndex=rows[0..] 不足 12 的部分（用 PipelineStage 默认填展示用）
            List<PromptTemplateStageVO> stageVos = new ArrayList<>();
            for (PipelineStage def : PipelineStage.ALL) {
                if (def == PipelineStage.PERSIST_ARTICLE) continue;
                PromptTemplateStage row = rows.stream()
                        .filter(r -> r.getStageIndex() != null && r.getStageIndex() == def.index)
                        .findFirst()
                        .orElse(null);
                if (row != null) {
                    stageVos.add(toStageVo(def, row));
                } else {
                    // 缺失的 stage 用 PipelineStage 默认填充（用于 UI 展示，实际不入库）
                    PromptTemplateStage virtual = new PromptTemplateStage();
                    virtual.setStageIndex(def.index);
                    virtual.setStageType(def.type.code);
                    virtual.setStageKey(def.key);
                    virtual.setAiPrompt(def.defaultAiPrompt);
                    virtual.setRuleConfig(def.defaultRuleConfigJson);
                    virtual.setEnabled(1);
                    stageVos.add(toStageVo(def, virtual));
                }
            }
            vo.setStages(stageVos);
            vo.setStagesInitialized(rows.size() == PipelineStage.ALL.length - 1);
        }
        vo.setIsBuiltin(t.getId() != null && t.getId() == CreativeTemplateConstants.DEFAULT_TEMPLATE_ID);
        TemplateStatus st = TemplateStatus.fromCode(t.getTemplateStatus());
        vo.setTemplateStatus(st.code);
        vo.setTemplateStatusLabel(st.label);
        vo.setLatestPublishedVersion(t.getLatestPublishedVersion());
        return vo;
    }

    private PromptTemplateStageVO toStageVo(PipelineStage def, PromptTemplateStage row) {
        PromptTemplateStageVO vo = new PromptTemplateStageVO();
        vo.setStageIndex(row.getStageIndex());
        vo.setStageType(row.getStageType() == null ? def.type.code : row.getStageType());
        vo.setStageKey(row.getStageKey() == null ? def.key : row.getStageKey());
        vo.setDisplayName(def.displayName);
        vo.setTypeLabel(def.type.label);
        vo.setDescription(def.description);
        vo.setEnabled(row.getEnabled() == null ? 1 : row.getEnabled());
        vo.setAiPrompt(row.getAiPrompt() != null ? row.getAiPrompt() : def.defaultAiPrompt);
        vo.setRuleConfig(row.getRuleConfig() != null ? row.getRuleConfig() : def.defaultRuleConfigJson);
        vo.setModelParams(parseModelParamsJson(row.getModelParams()));
        // 占位符
        List<PromptTemplateStageVO.StagePlaceholderVO> phs = new ArrayList<>();
        for (Placeholder p : def.placeholders) {
            PromptTemplateStageVO.StagePlaceholderVO ph = new PromptTemplateStageVO.StagePlaceholderVO();
            ph.setName(p.getName());
            ph.setDesc(p.getDesc());
            phs.add(ph);
        }
        vo.setPlaceholders(phs);
        // 规则阶段表单字段
        if (def.type == StageType.RULE_CONFIG) {
            List<PromptTemplateStageVO.StageConfigFieldVO> cfs = new ArrayList<>();
            for (ConfigField cf : def.configFields) {
                PromptTemplateStageVO.StageConfigFieldVO f = new PromptTemplateStageVO.StageConfigFieldVO();
                f.setKey(cf.getKey());
                f.setLabel(cf.getLabel());
                f.setType(cf.getType());
                f.setDefaultValue(cf.getDefaultValue());
                f.setMin(cf.getMin());
                f.setMax(cf.getMax());
                f.setDescription(cf.getDescription());
                if (cf.getOptions() != null) {
                    List<PromptTemplateStageVO.StageConfigFieldVO.Option> opts = new ArrayList<>();
                    for (ConfigField.Option o : cf.getOptions()) {
                        PromptTemplateStageVO.StageConfigFieldVO.Option opt =
                                new PromptTemplateStageVO.StageConfigFieldVO.Option();
                        opt.setLabel(o.getLabel());
                        opt.setValue(o.getValue());
                        opts.add(opt);
                    }
                    f.setOptions(opts);
                }
                cfs.add(f);
            }
            vo.setConfigFields(cfs);
        }
        return vo;
    }
}
