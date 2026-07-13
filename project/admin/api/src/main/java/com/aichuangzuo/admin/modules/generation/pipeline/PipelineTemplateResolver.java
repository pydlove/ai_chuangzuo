package com.aichuangzuo.admin.modules.generation.pipeline;

import com.aichuangzuo.admin.modules.generation.entity.PromptTemplateStage;
import com.aichuangzuo.admin.modules.generation.mapper.PromptTemplateMapper;
import com.aichuangzuo.admin.modules.generation.mapper.PromptTemplateStageMapper;
import com.aichuangzuo.admin.modules.generation.mapper.PromptTemplateVersionMapper;
import com.aichuangzuo.admin.modules.generation.service.PromptTemplateService;
import com.aichuangzuo.shared.entity.PromptTemplate;
import com.aichuangzuo.shared.entity.PromptTemplateVersion;
import com.aichuangzuo.shared.enums.error.AdminGenerationErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流水线模板解析器：从 t_prompt_template + t_prompt_template_stage 加载当前任务用的模板 + 12 阶段配置。
 *
 * <p>阶段 3 起：resolveInto(ctx, templateId, templateVersion) 接任务锁定的版本号。
 * 老任务（templateId=null 或 templateVersion=null）走 fallback：找当前唯一已发布（template_status=1）。
 *
 * <p>stage 表里没有的行（老模板没初始化）会用 {@link PipelineStage} 默认值兜底，保证 12 个 stage 一定齐全。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineTemplateResolver {

    private final PromptTemplateService templateService;
    private final PromptTemplateStageMapper stageMapper;
    private final PromptTemplateVersionMapper versionMapper;
    private final PromptTemplateMapper templateMapper;

    /**
     * 阶段 3 起：新签名，接收任务锁定的模板版本号。
     *
     * @param templateId      任务记录里的模板 ID（nullable，老任务）
     * @param templateVersion 任务记录里的锁定版本号（nullable，老任务）
     */
    public void resolveInto(GenerationContext ctx, Long templateId, Integer templateVersion) {
        PromptTemplate template;
        if (templateId != null && templateVersion != null) {
            // 锁定版本路径
            template = templateMapper.selectById(templateId);
            if (template == null) {
                throw new BusinessException(AdminGenerationErrorCode.PROMPT_TEMPLATE_NOT_FOUND);
            }
            // 找对应 version 的快照（仅供回溯）
            PromptTemplateVersion snapshot = versionMapper.selectByTemplateId(templateId).stream()
                    .filter(x -> x.getVersion() != null && x.getVersion().equals(templateVersion))
                    .findFirst()
                    .orElse(null);
            if (snapshot != null && snapshot.getConfigJson() != null) {
                ctx.setConfigJsonSnapshot(snapshot.getConfigJson());
            }
            log.debug("resolved template id={} version={} (locked)", templateId, templateVersion);
        } else {
            // fallback：找当前唯一已发布
            template = templateService.findPublished()
                    .orElseThrow(() -> new BusinessException(AdminGenerationErrorCode.PROMPT_TEMPLATE_NO_PUBLISHED));
            log.debug("resolved template id={} (fallback to published)", template.getId());
        }
        ctx.setTemplate(template);

        Map<Integer, PromptTemplateStage> stages = new HashMap<>();
        List<PromptTemplateStage> rows = stageMapper.selectByTemplateId(template.getId());
        for (PromptTemplateStage row : rows) {
            stages.put(row.getStageIndex(), row);
        }
        // 用 PipelineStage 默认值补齐缺失的 stage
        // （PERSIST_ARTICLE index=100 是 orchestrator 合成步骤，不进 stage 表，跳过）
        for (PipelineStage def : PipelineStage.ALL) {
            if (def == PipelineStage.PERSIST_ARTICLE) continue;
            stages.computeIfAbsent(def.index, idx -> buildDefaultStage(def, template.getId()));
        }
        ctx.setStages(stages);
        log.debug("resolved template id={} stages={} (defaults filled where missing)",
                template.getId(), stages.size());
    }

    /**
     * 兼容旧签名：自动走 fallback（templateId/version 都传 null）。
     */
    public void resolveInto(GenerationContext ctx) {
        resolveInto(ctx, null, null);
    }

    private PromptTemplateStage buildDefaultStage(PipelineStage def, Long templateId) {
        PromptTemplateStage s = new PromptTemplateStage();
        s.setTemplateId(templateId);
        s.setStageIndex(def.index);
        s.setStageType(def.type.code);
        s.setStageKey(def.key);
        s.setEnabled(1);
        if (def.type == StageType.AI_PROMPT) {
            s.setAiPrompt(def.defaultAiPrompt);
        } else if (def.type == StageType.RULE_CONFIG) {
            s.setRuleConfig(def.defaultRuleConfigJson);
        }
        s.setTenantId(0L);
        s.setIsDeleted(0);
        return s;
    }
}