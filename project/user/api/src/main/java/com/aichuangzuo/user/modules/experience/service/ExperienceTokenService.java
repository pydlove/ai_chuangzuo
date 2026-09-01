package com.aichuangzuo.user.modules.experience.service;

/**
 * 体验令牌服务。
 */
public interface ExperienceTokenService {

    /**
     * 消费体验令牌，为指定用户发放会员。
     *
     * @param userId 新注册用户ID
     * @param token  体验令牌
     */
    void consume(Long userId, String token);
}
