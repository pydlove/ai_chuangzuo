package com.aichuangzuo.user.modules.skill.service;

import com.aichuangzuo.user.modules.skill.vo.SystemSkillVO;

import java.util.List;

/**
 * 系统预设风格（{@code source_type=3}）服务。
 */
public interface SystemSkillService {

    /**
     * 列出当前启用的系统预设风格，可按名称关键词过滤。
     */
    List<SystemSkillVO> listEnabled(String keyword);
}