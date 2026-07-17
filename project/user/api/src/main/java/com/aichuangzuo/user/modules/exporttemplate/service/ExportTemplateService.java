package com.aichuangzuo.user.modules.exporttemplate.service;

import com.aichuangzuo.user.modules.exporttemplate.vo.ExportTemplateVO;

import java.util.List;

public interface ExportTemplateService {

    /** 查询启用中的模板列表（按 sort_order 升序）。 */
    List<ExportTemplateVO> listEnabled();
}
