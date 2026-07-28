package com.aichuangzuo.admin.modules.skill.preset.service;

import com.aichuangzuo.admin.modules.skill.preset.dto.request.CreateGlobalSkillRequest;
import com.aichuangzuo.admin.modules.skill.preset.dto.request.GlobalSkillPageRequest;
import com.aichuangzuo.admin.modules.skill.preset.dto.request.UpdateGlobalSkillRequest;
import com.aichuangzuo.admin.modules.skill.preset.vo.GlobalSkillVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 预设风格服务。
 */
public interface GlobalSkillService {

    /**
     * 分页查询系统预设风格。
     */
    IPage<GlobalSkillVO> page(GlobalSkillPageRequest request);

    /**
     * 创建系统预设风格，返回新生成的 bizNo。
     */
    String create(CreateGlobalSkillRequest request);

    /**
     * 更新系统预设风格（全量字段）。
     */
    void update(String bizNo, UpdateGlobalSkillRequest request);

    /**
     * 软删除（is_deleted=1）。
     */
    void delete(String bizNo);
}