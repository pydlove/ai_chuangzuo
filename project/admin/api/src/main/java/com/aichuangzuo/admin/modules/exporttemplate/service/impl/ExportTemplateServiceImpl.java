package com.aichuangzuo.admin.modules.exporttemplate.service.impl;

import com.aichuangzuo.admin.modules.exporttemplate.dto.request.ExportTemplateSaveRequest;
import com.aichuangzuo.admin.modules.exporttemplate.entity.ExportTemplate;
import com.aichuangzuo.admin.modules.exporttemplate.mapper.ExportTemplateMapper;
import com.aichuangzuo.admin.modules.exporttemplate.service.ExportTemplateService;
import com.aichuangzuo.admin.modules.exporttemplate.vo.ExportTemplateVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    @Override
    public List<ExportTemplateVO> listAll() {
        LambdaQueryWrapper<ExportTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ExportTemplate::getSortOrder);
        return exportTemplateMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public ExportTemplateVO getById(Long id) {
        ExportTemplate entity = exportTemplateMapper.selectById(id);
        return entity == null ? null : toVO(entity);
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

    private ExportTemplateVO toVO(ExportTemplate entity) {
        ExportTemplateVO vo = new ExportTemplateVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
