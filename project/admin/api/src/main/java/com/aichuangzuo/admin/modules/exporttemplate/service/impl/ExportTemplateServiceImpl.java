package com.aichuangzuo.admin.modules.exporttemplate.service.impl;

import com.aichuangzuo.admin.modules.exporttemplate.dto.request.ExportTemplateParamSaveRequest;
import com.aichuangzuo.admin.modules.exporttemplate.dto.request.ExportTemplateSaveRequest;
import com.aichuangzuo.admin.modules.exporttemplate.entity.ExportTemplate;
import com.aichuangzuo.admin.modules.exporttemplate.entity.ExportTemplateParam;
import com.aichuangzuo.admin.modules.exporttemplate.mapper.ExportTemplateMapper;
import com.aichuangzuo.admin.modules.exporttemplate.mapper.ExportTemplateParamMapper;
import com.aichuangzuo.admin.modules.exporttemplate.service.ExportTemplateService;
import com.aichuangzuo.admin.modules.exporttemplate.vo.ExportTemplateParamVO;
import com.aichuangzuo.admin.modules.exporttemplate.vo.ExportTemplateVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportTemplateServiceImpl implements ExportTemplateService {

    private final ExportTemplateMapper exportTemplateMapper;
    private final ExportTemplateParamMapper paramMapper;
    private final ObjectMapper objectMapper;

    // ===== 模板 CRUD =====

    @Override
    public List<ExportTemplateVO> listAll() {
        LambdaQueryWrapper<ExportTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ExportTemplate::getSortOrder);
        return exportTemplateMapper.selectList(wrapper).stream()
                .map(this::toTemplateVO)
                .collect(Collectors.toList());
    }

    @Override
    public ExportTemplateVO getById(Long id) {
        ExportTemplate entity = exportTemplateMapper.selectById(id);
        return entity == null ? null : toTemplateVO(entity);
    }

    @Override
    public void save(ExportTemplateSaveRequest request) {
        ExportTemplate entity = new ExportTemplate();
        BeanUtils.copyProperties(request, entity);
        exportTemplateMapper.insert(entity);
        log.info("新增导出模板 key={} name={}", entity.getTemplateKey(), entity.getName());
    }

    @Override
    public void update(Long id, ExportTemplateSaveRequest request) {
        ExportTemplate entity = exportTemplateMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("模板不存在 id=" + id);
        }
        BeanUtils.copyProperties(request, entity);
        entity.setId(id);
        exportTemplateMapper.updateById(entity);
        log.info("更新导出模板 id={} key={}", id, entity.getTemplateKey());
    }

    @Override
    public void delete(Long id) {
        exportTemplateMapper.deleteById(id);
        log.info("删除导出模板 id={}", id);
    }

    // ===== 参数定义 CRUD =====

    @Override
    public List<ExportTemplateParamVO> listParams() {
        LambdaQueryWrapper<ExportTemplateParam> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ExportTemplateParam::getGroupLabel)
                .orderByAsc(ExportTemplateParam::getSortOrder);
        return paramMapper.selectList(wrapper).stream()
                .map(p -> ExportTemplateParamVO.from(p, objectMapper))
                .collect(Collectors.toList());
    }

    @Override
    public void saveParam(ExportTemplateParamSaveRequest request) {
        ExportTemplateParam entity = new ExportTemplateParam();
        BeanUtils.copyProperties(request, entity);
        paramMapper.insert(entity);
        log.info("新增导出模板参数 key={} fieldType={}", entity.getParamKey(), entity.getFieldType());
    }

    @Override
    public void updateParam(Long id, ExportTemplateParamSaveRequest request) {
        ExportTemplateParam entity = paramMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("参数不存在 id=" + id);
        }
        BeanUtils.copyProperties(request, entity);
        entity.setId(id);
        paramMapper.updateById(entity);
        log.info("更新导出模板参数 id={} key={}", id, entity.getParamKey());
    }

    @Override
    public void deleteParam(Long id) {
        paramMapper.deleteById(id);
        log.info("删除导出模板参数 id={}", id);
    }

    private ExportTemplateVO toTemplateVO(ExportTemplate entity) {
        ExportTemplateVO vo = new ExportTemplateVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}