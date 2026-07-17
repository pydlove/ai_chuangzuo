package com.aichuangzuo.user.modules.exporttemplate.service.impl;

import com.aichuangzuo.user.modules.exporttemplate.entity.ExportTemplate;
import com.aichuangzuo.user.modules.exporttemplate.mapper.ExportTemplateMapper;
import com.aichuangzuo.user.modules.exporttemplate.service.ExportTemplateService;
import com.aichuangzuo.user.modules.exporttemplate.vo.ExportTemplateVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportTemplateServiceImpl implements ExportTemplateService {

    private final ExportTemplateMapper exportTemplateMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<ExportTemplateVO> listEnabled() {
        LambdaQueryWrapper<ExportTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExportTemplate::getStatus, 1)
                .orderByAsc(ExportTemplate::getSortOrder);
        return exportTemplateMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private ExportTemplateVO toVO(ExportTemplate entity) {
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
