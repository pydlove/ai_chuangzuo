package com.aichuangzuo.user.modules.skill.market.service;

/**
 * 用户端 - 风格市场写操作服务。
 */
public interface SkillMarketCommandService {

    /**
     * 发布者删除（下架）自己的市场 skill。
     *
     * @param bizNo  市场 skill 业务编号
     * @param userId 当前用户ID
     */
    void deleteOwnMarketSkill(String bizNo, Long userId);
}
