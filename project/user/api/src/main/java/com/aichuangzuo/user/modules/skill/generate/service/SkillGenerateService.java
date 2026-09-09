package com.aichuangzuo.user.modules.skill.generate.service;

import com.aichuangzuo.user.modules.skill.generate.vo.SkillGenerateStatusVO;
import com.aichuangzuo.user.modules.skill.generate.vo.SkillGenerateVO;

/**
 * 小爱帮写服务。
 */
public interface SkillGenerateService {

    /**
     * 查询当前用户的帮写状态（套餐权限 + 当日剩余次数）。
     *
     * @param userId 用户ID
     * @return 帮写状态
     */
    SkillGenerateStatusVO status(Long userId);

    /**
     * 根据运营方案或描述方向 AI 生成结构化提示词。
     *
     * <p>专业版及以上可用，每日限 4 次。
     *
     * @param userId      用户ID
     * @param requirement 运营方案或提示词描述方向
     * @return 生成结果（含今日剩余次数）
     */
    SkillGenerateVO generate(Long userId, String requirement);
}
