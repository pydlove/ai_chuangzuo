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
     * 根据生成任务 ID 查询作品详情（工作台查看兜底）。
     */
    ArticleVO getByTaskId(Long userId, Long taskId);

    /**
     * 内部查询：管理端通过 articleBizNo 读取作品内容（不校验用户归属）。
     */
    ArticleVO getInternal(String bizNo);

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

    /**
     * 生成单篇作品的临时导出 token（用于免登录下载）。
     */
    String generateExportToken(Long userId, String bizNo);

    /**
     * 解析并校验临时导出 token，返回作品编号。
     */
    String parseExportToken(String token);

    /**
     * 根据作品编号导出为 Word 文件字节数组。
     */
    byte[] exportAsWord(String bizNo);
}