package com.aichuangzuo.user.modules.article.service;

import com.aichuangzuo.user.modules.article.dto.request.SaveArticleRequest;
import com.aichuangzuo.user.modules.article.dto.request.UpdateArticleRequest;
import com.aichuangzuo.user.modules.article.vo.ArticlePageVO;
import com.aichuangzuo.user.modules.article.vo.ArticleVO;

/**
 * 用户作品服务。
 */
public interface ArticleService {

    /**
     * 分页查询当前用户的作品。
     */
    ArticlePageVO list(Long userId, String keyword, long page, long pageSize);

    /**
     * 查询单篇作品详情。
     */
    ArticleVO get(Long userId, String bizNo);

    /**
     * 创建作品（生成完成时调用）。
     *
     * @return 新作品的 bizNo
     */
    String save(Long userId, SaveArticleRequest request);

    /**
     * 修改作品（编辑保存时调用）。
     */
    void update(Long userId, String bizNo, UpdateArticleRequest request);

    /**
     * 软删除作品。
     */
    void delete(Long userId, String bizNo);

    /**
     * 查询用户本月已生成作品数。
     */
    Long monthlyCount(Long userId);
}