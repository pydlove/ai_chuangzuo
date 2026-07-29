package com.aichuangzuo.admin.modules.exporttemplate.service;

import com.aichuangzuo.admin.modules.exporttemplate.dto.request.ExportTemplateParamSaveRequest;
import com.aichuangzuo.admin.modules.exporttemplate.dto.request.ExportTemplateSaveRequest;
import com.aichuangzuo.admin.modules.exporttemplate.vo.ExportTemplateParamVO;
import com.aichuangzuo.admin.modules.exporttemplate.vo.ExportTemplateVO;

import java.util.List;

public interface ExportTemplateService {

    // ===== 模板 CRUD =====

    List<ExportTemplateVO> listAll();

    ExportTemplateVO getById(Long id);

    void save(ExportTemplateSaveRequest request);

    void update(Long id, ExportTemplateSaveRequest request);

    void delete(Long id);

    // ===== 参数定义 CRUD（管理端可增删改查参数）=====

    /** 列出所有参数定义，按 group + sortOrder 排序。 */
    List<ExportTemplateParamVO> listParams();

    void saveParam(ExportTemplateParamSaveRequest request);

    void updateParam(Long id, ExportTemplateParamSaveRequest request);

    void deleteParam(Long id);
}