package com.aichuangzuo.admin.modules.learn.service;

import com.aichuangzuo.admin.modules.learn.dto.request.LearnCategoryReq;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnSortReq;
import com.aichuangzuo.admin.modules.learn.vo.LearnCategoryTreeNode;

import java.util.List;

public interface LearnCategoryService {

    List<LearnCategoryTreeNode> tree();

    Long create(LearnCategoryReq req);

    void update(Long id, LearnCategoryReq req);

    void delete(Long id);

    void sortBatch(List<LearnSortReq.SortItem> items);
}
