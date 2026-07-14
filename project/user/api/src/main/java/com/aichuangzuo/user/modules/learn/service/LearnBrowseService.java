package com.aichuangzuo.user.modules.learn.service;

import com.aichuangzuo.user.modules.learn.vo.LearnArticleVO;
import com.aichuangzuo.user.modules.learn.vo.LearnBannerVO;
import com.aichuangzuo.user.modules.learn.vo.LearnCategoryDetailVO;
import com.aichuangzuo.user.modules.learn.vo.LearnCategoryTreeVO;

import java.util.List;

public interface LearnBrowseService {

    /** 分类树（仅保留有已发布文章的路径） */
    List<LearnCategoryTreeVO> tree();

    /** 分类详情 + 子分类 + 已发布文章分页列表 */
    LearnCategoryDetailVO categoryDetail(Long id, int page, int size);

    /** 文章详情；草稿等同 null（让 Controller 转 404） */
    LearnArticleVO articleDetail(Long id);

    /** Banner 列表（所有未删除，按 sort ASC） */
    List<LearnBannerVO> banners();
}
