package com.aichuangzuo.admin.modules.learn.service.impl;

import com.aichuangzuo.admin.modules.learn.dto.request.LearnCategoryReq;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnSortReq;
import com.aichuangzuo.admin.modules.learn.entity.LearnCategoryEntity;
import com.aichuangzuo.admin.modules.learn.exception.LearnErrorCode;
import com.aichuangzuo.admin.modules.learn.mapper.LearnCategoryMapper;
import com.aichuangzuo.admin.modules.learn.service.LearnCategoryService;
import com.aichuangzuo.admin.modules.learn.vo.LearnCategoryTreeNode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearnCategoryServiceImpl implements LearnCategoryService {

    private final LearnCategoryMapper mapper;

    @Override
    public List<LearnCategoryTreeNode> tree() {
        // MyBatis-Plus @TableLogic 自动加 is_deleted=0 过滤
        List<LearnCategoryEntity> all = mapper.selectList(null);
        if (all.isEmpty()) return List.of();

        Map<Long, LearnCategoryTreeNode> nodeMap = all.stream().collect(Collectors.toMap(
                LearnCategoryEntity::getId,
                e -> {
                    LearnCategoryTreeNode n = new LearnCategoryTreeNode();
                    n.setId(e.getId());
                    n.setParentId(e.getParentId());
                    n.setName(e.getName());
                    n.setSort(e.getSort());
                    n.setIsRecommended(e.getIsRecommended());
                    return n;
                }));

        List<LearnCategoryTreeNode> roots = new ArrayList<>();
        for (LearnCategoryEntity e : all) {
            LearnCategoryTreeNode node = nodeMap.get(e.getId());
            if (e.getParentId() == null) {
                roots.add(node);
            } else {
                LearnCategoryTreeNode parent = nodeMap.get(e.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    // 父节点缺失（被删等）兜底挂顶级
                    roots.add(node);
                }
            }
        }
        sortRecursively(roots);
        return roots;
    }

    private void sortRecursively(List<LearnCategoryTreeNode> nodes) {
        if (nodes == null || nodes.isEmpty()) return;
        nodes.sort(Comparator.comparing(LearnCategoryTreeNode::getSort));
        nodes.forEach(n -> sortRecursively(n.getChildren()));
    }

    @Override
    public Long create(LearnCategoryReq req) {
        rejectDuplicateName(req.getParentId(), req.getName(), null);
        LearnCategoryEntity entity = new LearnCategoryEntity();
        entity.setParentId(req.getParentId());
        entity.setName(req.getName());
        entity.setSort(req.getSort() != null ? req.getSort() : 0);
        mapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long id, LearnCategoryReq req) {
        LearnCategoryEntity exist = requireExisting(id);
        rejectDuplicateName(req.getParentId(), req.getName(), id);
        exist.setParentId(req.getParentId());
        exist.setName(req.getName());
        exist.setSort(req.getSort() != null ? req.getSort() : 0);
        exist.setIsRecommended(req.getIsRecommended() != null ? req.getIsRecommended() : 0);
        mapper.updateById(exist);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireExisting(id);
        // 子分类校验
        Long childCount = mapper.selectCount(new QueryWrapper<LearnCategoryEntity>()
                .eq("parent_id", id));
        if (childCount > 0) {
            throw new BusinessException(LearnErrorCode.CATEGORY_NOT_EMPTY);
        }
        mapper.deleteById(id); // @TableLogic 软删除
    }

    @Override
    @Transactional
    public void sortBatch(List<LearnSortReq.SortItem> items) {
        for (LearnSortReq.SortItem it : items) {
            LearnCategoryEntity e = requireExisting(it.getId());
            e.setSort(it.getSort());
            if (it.getParentId() != null) e.setParentId(it.getParentId());
            mapper.updateById(e);
        }
    }

    // -------- helpers --------

    private void rejectDuplicateName(Long parentId, String name, Long excludeId) {
        QueryWrapper<LearnCategoryEntity> qw = new QueryWrapper<LearnCategoryEntity>()
                .eq("name", name);
        if (parentId == null) {
            qw.isNull("parent_id");
        } else {
            qw.eq("parent_id", parentId);
        }
        LearnCategoryEntity exist = mapper.selectOne(qw);
        if (exist != null && !exist.getId().equals(excludeId)) {
            throw new BusinessException(LearnErrorCode.CATEGORY_NAME_DUPLICATE);
        }
    }

    private LearnCategoryEntity requireExisting(Long id) {
        LearnCategoryEntity e = mapper.selectById(id);
        if (e == null) {
            throw new BusinessException(LearnErrorCode.CATEGORY_NOT_FOUND);
        }
        return e;
    }
}
