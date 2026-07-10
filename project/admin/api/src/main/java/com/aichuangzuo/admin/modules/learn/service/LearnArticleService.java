package com.aichuangzuo.admin.modules.learn.service;

import com.aichuangzuo.admin.modules.learn.dto.request.LearnArticlePageQuery;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnArticleReq;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnSortReq;
import com.aichuangzuo.admin.modules.learn.vo.LearnArticleDetail;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface LearnArticleService {

    IPage<LearnArticleDetail> page(LearnArticlePageQuery q);

    LearnArticleDetail detail(Long id);

    Long create(LearnArticleReq req);

    void update(Long id, LearnArticleReq req);

    void delete(Long id);

    void publish(Long id);

    void unpublish(Long id);

    void move(Long id, Long categoryId);

    void sortBatch(List<LearnSortReq.SortItem> items);
}
