package com.aichuangzuo.user.modules.skill.service;

import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
import com.aichuangzuo.user.modules.skill.vo.SkillAnalyzeVO;

/**
 * 风格分析服务：调大模型拆解参考文章并提取风格。
 */
public interface SkillAnalyzeService {

    /**
     * 预扣一次 skill_learn_analyze 月度额度。
     *
     * @param userId 当前用户 ID
     * @return 预扣结果
     */
    BenefitCheckVO preConsume(Long userId);

    /**
     * 分析参考文章写作风格并提取风格提示词。
     * 注意：本方法不再消费额度，请先在调用前执行 {@link #preConsume(Long)}。
     *
     * @param userId 当前用户 ID
     * @param text 参考文章正文（200-1000 字，Controller 层已校验）
     * @return 风格提示词 + 2 段原文摘录
     */
    SkillAnalyzeVO analyze(Long userId, String text);

    /**
     * 确认扣减：将预扣额度转为正式用量。
     *
     * @param userId 当前用户 ID
     */
    void confirmConsume(Long userId);

    /**
     * 释放预扣额度（用户取消或关闭弹框时调用）。
     *
     * @param userId 当前用户 ID
     */
    void cancelConsume(Long userId);
}
