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
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportTemplateServiceImpl implements ExportTemplateService {

    /** template_access 权益编码：逗号分隔的 template_key 列表。 */
    private static final String BENEFIT_TEMPLATE_ACCESS = "template_access";

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
        return rows.stream()
                .map(t -> toVO(t, accessibleKeys.contains(t.getTemplateKey())))
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
