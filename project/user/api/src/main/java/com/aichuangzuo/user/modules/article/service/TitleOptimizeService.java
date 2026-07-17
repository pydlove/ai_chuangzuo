package com.aichuangzuo.user.modules.article.service;

import com.aichuangzuo.user.modules.article.vo.TitleOptimizeVO;

/**
 * AI 标题优化服务：按平台生成优化标题，首次调用大模型，之后返回缓存。
 */
public interface TitleOptimizeService {

    /**
     * 为指定作品生成（或返回缓存的）分平台优化标题。
     *
     * @param userId 当前用户
     * @param bizNo  作品 bizNo
     */
    TitleOptimizeVO optimize(Long userId, String bizNo);
}
