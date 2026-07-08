package com.aichuangzuo.admin.modules.style.preset.service;

import com.aichuangzuo.admin.modules.style.preset.dto.request.CreateGlobalStyleRequest;
import com.aichuangzuo.admin.modules.style.preset.dto.request.GlobalStylePageRequest;
import com.aichuangzuo.admin.modules.style.preset.dto.request.UpdateGlobalStyleRequest;
import com.aichuangzuo.admin.modules.style.preset.vo.GlobalStyleVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 预设风格服务。
 */
public interface GlobalStyleService {

    /**
     * 分页查询系统预设风格。
     */
    IPage<GlobalStyleVO> page(GlobalStylePageRequest request);

    /**
     * 创建系统预设风格，返回新生成的 bizNo。
     */
    String create(CreateGlobalStyleRequest request);

    /**
     * 更新系统预设风格（全量字段）。
     */
    void update(String bizNo, UpdateGlobalStyleRequest request);

    /**
     * 软删除（is_deleted=1）。
     */
    void delete(String bizNo);
}