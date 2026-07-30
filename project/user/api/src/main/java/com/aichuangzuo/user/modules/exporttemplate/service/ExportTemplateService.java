package com.aichuangzuo.user.modules.exporttemplate.service;

import com.aichuangzuo.user.modules.exporttemplate.vo.ExportTemplateVO;

import java.util.List;

public interface ExportTemplateService {

    /**
     * 查询启用中的模板列表（按 sort_order 升序），并按当前用户套餐的 template_access 权益
     * 给每个模板打 accessible 标记。
     *
     * @param userId 当前用户 id，可为 null（未登录 / 公开访问）
     */
    List<ExportTemplateVO> listEnabled(Long userId);
}
