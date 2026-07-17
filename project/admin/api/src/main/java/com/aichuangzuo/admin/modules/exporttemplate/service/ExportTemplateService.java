package com.aichuangzuo.admin.modules.exporttemplate.service;

import com.aichuangzuo.admin.modules.exporttemplate.dto.request.ExportTemplateSaveRequest;
import com.aichuangzuo.admin.modules.exporttemplate.vo.ExportTemplateVO;

import java.util.List;

public interface ExportTemplateService {

    List<ExportTemplateVO> listAll();

    ExportTemplateVO getById(Long id);

    void save(ExportTemplateSaveRequest request);

    void update(Long id, ExportTemplateSaveRequest request);

    void delete(Long id);
}
