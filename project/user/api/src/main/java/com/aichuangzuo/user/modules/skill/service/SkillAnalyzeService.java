package com.aichuangzuo.user.modules.skill.service;

import com.aichuangzuo.user.modules.skill.vo.SkillAnalyzeVO;

/**
 * 风格分析服务：调大模型拆解参考文章并提取风格。
 */
public interface SkillAnalyzeService {

    /**
     * 分析参考文章写作风格并提取风格提示词。
     *
     * @param userId 当前用户 ID（用于消费 skill_learn_analyze 月度额度）
     * @param text 参考文章正文（200-3000 字，Controller 层已校验）
     * @return 风格提示词 + 2 段原文摘录
     */
    SkillAnalyzeVO analyze(Long userId, String text);
}
