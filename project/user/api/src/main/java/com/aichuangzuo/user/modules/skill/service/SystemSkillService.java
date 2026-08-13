package com.aichuangzuo.user.modules.skill.service;

import com.aichuangzuo.user.modules.skill.vo.SystemSkillVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 系统预设风格（{@code source_type=3}）服务。
 */
public interface SystemSkillService {

    /**
     * 分页列出当前启用的系统预设风格，可按名称关键词过滤。
     *
     * @param keyword  关键词，匹配名称；为空时不过滤
     * @param page     页码，从 1 开始
     * @param pageSize 每页条数
     * @return 分页系统预设风格列表
     */
    IPage<SystemSkillVO> listEnabled(String keyword, int page, int pageSize);
}