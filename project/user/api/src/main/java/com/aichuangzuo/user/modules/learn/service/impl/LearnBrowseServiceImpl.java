package com.aichuangzuo.user.modules.learn.service.impl;

import com.aichuangzuo.user.modules.learn.entity.LearnArticleEntity;
import com.aichuangzuo.user.modules.learn.entity.LearnCategoryEntity;
import com.aichuangzuo.user.modules.learn.enums.ArticleStatus;
import com.aichuangzuo.user.modules.learn.enums.ContentType;
import com.aichuangzuo.user.modules.learn.mapper.LearnArticleMapper;
import com.aichuangzuo.user.modules.learn.mapper.LearnCategoryMapper;
import com.aichuangzuo.user.modules.learn.service.LearnBrowseService;
import com.aichuangzuo.user.modules.learn.vo.LearnArticleVO;
import com.aichuangzuo.user.modules.learn.vo.LearnCategoryDetailVO;
import com.aichuangzuo.user.modules.learn.vo.LearnCategoryTreeVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearnBrowseServiceImpl implements LearnBrowseService {

    private final LearnCategoryMapper categoryMapper;
    private final LearnArticleMapper articleMapper;

    @Override
    public List<LearnCategoryTreeVO> tree() {
        List<LearnCategoryEntity> allCats = categoryMapper.selectList(null); // @TableLogic 自动过滤
        if (allCats.isEmpty()) return List.of();

        // 已发布文章涉及到的分类
        Set<Long> leafIds = articleMapper.selectList(new QueryWrapper<LearnArticleEntity>()
                        .eq("status", ArticleStatus.PUBLISHED.getCode()))
                .stream()
                .map(LearnArticleEntity::getCategoryId)
                .collect(Collectors.toSet());

        if (leafIds.isEmpty()) return List.of();

        // 父级分类一并保留
        Map<Long, LearnCategoryEntity> byId = allCats.stream()
                .collect(Collectors.toMap(LearnCategoryEntity::getId, c -> c));
        Set<Long> keep = new HashSet<>(leafIds);
        for (Long cid : leafIds) {
            LearnCategoryEntity c = byId.get(cid);
            while (c != null && c.getParentId() != null) {
                keep.add(c.getId());
                c = byId.get(c.getParentId());
            }
            keep.add(cid); // 确保叶子节点自身也算入
        }

        List<LearnCategoryEntity> filtered = allCats.stream()
                .filter(c -> keep.contains(c.getId())).toList();
        return buildTree(filtered);
    }

    private List<LearnCategoryTreeVO> buildTree(List<LearnCategoryEntity> nodes) {
        Map<Long, LearnCategoryTreeVO> map = new LinkedHashMap<>();
        for (LearnCategoryEntity e : nodes) {
            LearnCategoryTreeVO n = new LearnCategoryTreeVO();
            n.setId(e.getId());
            n.setParentId(e.getParentId());
            n.setName(e.getName());
            n.setSort(e.getSort());
            map.put(e.getId(), n);
        }
        List<LearnCategoryTreeVO> roots = new ArrayList<>();
        for (LearnCategoryEntity e : nodes) {
            LearnCategoryTreeVO n = map.get(e.getId());
            if (e.getParentId() == null) {
                roots.add(n);
            } else {
                LearnCategoryTreeVO p = map.get(e.getParentId());
                if (p != null) {
                    p.getChildren().add(n);
                } else {
                    roots.add(n);
                }
            }
        }
        sortRecursive(roots);
        return roots;
    }

    private void sortRecursive(List<LearnCategoryTreeVO> nodes) {
        if (nodes == null || nodes.isEmpty()) return;
        nodes.sort(Comparator.comparing(LearnCategoryTreeVO::getSort));
        nodes.forEach(n -> sortRecursive(n.getChildren()));
    }

    @Override
    public LearnCategoryDetailVO categoryDetail(Long id, int page, int size) {
        LearnCategoryEntity cat = categoryMapper.selectById(id);
        if (cat == null) return null;

        LearnCategoryDetailVO vo = new LearnCategoryDetailVO();
        vo.setId(cat.getId());
        vo.setName(cat.getName());
        vo.setParentId(cat.getParentId());
        if (cat.getParentId() != null) {
            LearnCategoryEntity parent = categoryMapper.selectById(cat.getParentId());
            if (parent != null) vo.setParentName(parent.getName());
        }
        vo.setChildren(buildTree(categoryMapper.selectList(
                new QueryWrapper<LearnCategoryEntity>().eq("parent_id", id))));
        vo.setPage(page);
        vo.setSize(size);

        Page<LearnArticleEntity> p = new Page<>(page, size);
        Page<LearnArticleEntity> res = articleMapper.selectPage(p, new QueryWrapper<LearnArticleEntity>()
                .eq("status", ArticleStatus.PUBLISHED.getCode())
                .eq("category_id", id)
                .orderByAsc("sort")
                .orderByDesc("updated_at"));
        vo.setArticles(res.getRecords().stream().map(this::toVo).toList());
        vo.setTotal(res.getTotal());
        return vo;
    }

    @Override
    public LearnArticleVO articleDetail(Long id) {
        LearnArticleEntity e = articleMapper.selectOne(new QueryWrapper<LearnArticleEntity>()
                .eq("id", id)
                .eq("status", ArticleStatus.PUBLISHED.getCode()));
        return e == null ? null : toVo(e);
    }

    private LearnArticleVO toVo(LearnArticleEntity e) {
        LearnArticleVO v = new LearnArticleVO();
        v.setId(e.getId());
        v.setCategoryId(e.getCategoryId());
        v.setTitle(e.getTitle());
        v.setSummary(e.getSummary());
        v.setContentType(e.getContentType());
        v.setContent(e.getContent());
        v.setPublishedAt(e.getPublishedAt());
        v.setUpdatedAt(e.getUpdatedAt());
        return v;
    }
}
