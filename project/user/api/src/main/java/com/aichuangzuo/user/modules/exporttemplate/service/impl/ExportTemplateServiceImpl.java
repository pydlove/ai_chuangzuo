package com.aichuangzuo.user.modules.exporttemplate.service.impl;

import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.exporttemplate.entity.ExportTemplate;
import com.aichuangzuo.user.modules.exporttemplate.mapper.ExportTemplateMapper;
import com.aichuangzuo.user.modules.exporttemplate.service.ExportTemplateService;
import com.aichuangzuo.user.modules.exporttemplate.vo.ExportTemplateVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportTemplateServiceImpl implements ExportTemplateService {

    /** template_access 权益编码：逗号分隔的 template_key 列表。 */
    private static final String BENEFIT_TEMPLATE_ACCESS = "template_access";

    /** 模板所需套餐层级 rank，用于 template_access 为空/失效时的兜底判断。 */
    private static final Map<String, Integer> TIER_RANK = Map.of(
            "basic", 0,
            "pro", 1,
            "flagship", 2
    );

    private final ExportTemplateMapper exportTemplateMapper;
    private final BenefitService benefitService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<ExportTemplateVO> listEnabled(Long userId) {
        LambdaQueryWrapper<ExportTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExportTemplate::getStatus, 1)
                .orderByAsc(ExportTemplate::getSortOrder);
        List<ExportTemplate> rows = exportTemplateMapper.selectList(wrapper);
        Set<String> accessibleKeys = resolveAccessibleKeys(userId);
        TemplateAccessPolicy policy = buildAccessPolicy(userId, rows, accessibleKeys);
        return rows.stream()
                .map(t -> toVO(t, policy.isAccessible(t)))
                .collect(Collectors.toList());
    }

    /**
     * 解析当前用户套餐的 template_access 权益值，得到可访问的 template_key 集合。
     * 未登录 / 已过期 / 权益为空 → 返回空集合（前端全部加锁）。
     */
    private Set<String> resolveAccessibleKeys(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        String raw = benefitService.getPlanBenefitValue(userId, BENEFIT_TEMPLATE_ACCESS, "");
        if (!StringUtils.hasText(raw)) {
            return Collections.emptySet();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * 构建模板访问策略。
     * 正常情况以 template_access 逗号列表为准；当该权益为空或不含任何现有模板 key 时
     *（例如旧枚举值 basic_8 / all_20 / all_custom 残留、管理端误清空），
     * 按模板自身 tier 兜底：用户套餐 tier 等级 ≥ 模板 tier 等级即可访问。
     */
    private TemplateAccessPolicy buildAccessPolicy(Long userId, List<ExportTemplate> rows, Set<String> accessibleKeys) {
        String planKey = userId == null ? "free" : benefitService.getCurrentPlanKey(userId);
        if (!TIER_RANK.containsKey(planKey)) {
            return new TemplateAccessPolicy(accessibleKeys, false, Integer.MAX_VALUE);
        }
        Set<String> templateKeys = rows.stream()
                .map(ExportTemplate::getTemplateKey)
                .collect(Collectors.toSet());
        boolean fallbackByTier = accessibleKeys.isEmpty()
                || Collections.disjoint(accessibleKeys, templateKeys);
        return new TemplateAccessPolicy(accessibleKeys, fallbackByTier, TIER_RANK.get(planKey));
    }

    private record TemplateAccessPolicy(Set<String> explicitKeys, boolean fallbackByTier, int userTierRank) {
        boolean isAccessible(ExportTemplate template) {
            if (explicitKeys.contains(template.getTemplateKey())) {
                return true;
            }
            if (!fallbackByTier) {
                return false;
            }
            int templateRank = TIER_RANK.getOrDefault(template.getTier(), Integer.MAX_VALUE);
            return templateRank <= userTierRank;
        }
    }

    private ExportTemplateVO toVO(ExportTemplate entity, boolean accessible) {
        ExportTemplateVO vo = new ExportTemplateVO();
        vo.setTemplateKey(entity.getTemplateKey());
        vo.setName(entity.getName());
        vo.setPlatform(entity.getPlatform());
        vo.setDescription(entity.getDescription());
        vo.setBgColor(entity.getBgColor());
        vo.setTextColor(entity.getTextColor());
        vo.setSignatureText(entity.getSignatureText());
        vo.setSignaturePosition(entity.getSignaturePosition());
        vo.setSortOrder(entity.getSortOrder());
        vo.setTier(entity.getTier());
        vo.setAccessible(accessible);
        // visualStyleJson 解析为 Object 返回，前端直接用
        if (entity.getVisualStyleJson() != null && !entity.getVisualStyleJson().isBlank()) {
            try {
                vo.setVisualStyle(objectMapper.readValue(entity.getVisualStyleJson(), Object.class));
            } catch (Exception e) {
                log.warn("模板 visualStyleJson 解析失败 key={}", entity.getTemplateKey(), e);
            }
        }
        return vo;
    }
}
