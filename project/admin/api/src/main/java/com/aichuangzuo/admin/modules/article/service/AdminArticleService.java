package com.aichuangzuo.admin.modules.article.service;

import com.aichuangzuo.admin.modules.article.vo.AdminArticleDetailVO;
import com.aichuangzuo.admin.modules.article.vo.AdminArticlePageVO;

/**
 * 管理端用户作品服务。
 */
public interface AdminArticleService {

    /**
     * 分页查询用户作品列表。
     *
     * @param userId 用户ID
     * @param keyword 搜索关键词（标题/描述）
     * @param page 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    AdminArticlePageVO listUserArticles(Long userId, String keyword, int page, int pageSize);

    /**
     * 查询作品详情。
     *
     * @param bizNo 业务编号
     * @return 作品详情
     */
    AdminArticleDetailVO getArticleDetail(String bizNo);
}
