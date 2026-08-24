package com.aichuangzuo.user.modules.workbench.service;

import com.aichuangzuo.user.modules.workbench.dto.request.SaveWeeklyArticlesRequest;
import com.aichuangzuo.user.modules.workbench.vo.WeeklyArticleVO;

import java.util.List;

/**
 * 工作台每周文章数据服务。
 */
public interface WeeklyArticleService {

    /**
     * 查询当前用户当前周的文章数据。
     */
    List<WeeklyArticleVO> getCurrentWeekArticles(Long userId);

    /**
     * 保存当前用户当前周的文章数据（整单替换）。
     */
    void saveCurrentWeekArticles(Long userId, SaveWeeklyArticlesRequest request);
}
