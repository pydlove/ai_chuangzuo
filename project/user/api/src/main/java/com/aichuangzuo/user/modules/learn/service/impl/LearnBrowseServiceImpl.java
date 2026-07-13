package com.aichuangzuo.user.modules.learn.service.impl;

import com.aichuangzuo.user.modules.learn.entity.LearnArticleEntity;
import com.aichuangzuo.user.modules.learn.entity.LearnCategoryEntity;
import com.aichuangzuo.user.modules.learn.enums.ArticleStatus;
import com.aichuangzuo.user.modules.learn.enums.ContentType;
import com.aichuangzuo.user.modules.learn.mapper.LearnArticleMapper;
import com.aichuangzuo.user.modules.learn.mapper.LearnCategoryMapper;
import com.aichuangzuo.user.modules.learn.service.LearnBrowseService;
import com.aichuangzuo.user.modules.learn.vo.LearnArticleRefVO;
import com.aichuangzuo.user.modules.learn.vo.LearnArticleVO;
import com.aichuangzuo.user.modules.learn.vo.LearnCategoryDetailVO;
import com.aichuangzuo.user.modules.learn.vo.LearnCategoryTreeVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class LearnBrowseServiceImpl implements LearnBrowseService {

    private final LearnCategoryMapper categoryMapper;
    private final LearnArticleMapper articleMapper;

    @Override
    public List<LearnCategoryTreeVO> tree() {
        // 分类树始终返回全部有效分类（@TableLogic 自动过滤 is_deleted）。
        // 不管分类下是否已有 PUBLISHED 文章，都要展示出来，让管理端建好的分类在用户端立即可见。
        // 空分类由前端显示"内容筹备中"占位，而不是后端过滤掉。
        List<LearnCategoryEntity> allCats = categoryMapper.selectList(null);
        if (allCats.isEmpty()) return List.of();
        return buildTree(allCats);
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
        LearnArticleEntity current = articleMapper.selectOne(new QueryWrapper<LearnArticleEntity>()
                .eq("id", id)
                .eq("status", ArticleStatus.PUBLISHED.getCode()));
        if (current == null) return null;

        LearnArticleVO vo = toVo(current);

        List<LearnArticleEntity> chain = buildReadingChain();
        Map<Long, String> catNames = loadCategoryNames();

        int idx = -1;
        for (int i = 0; i < chain.size(); i++) {
            if (chain.get(i).getId().equals(id)) { idx = i; break; }
        }
        if (idx > 0) {
            vo.setPrevArticle(toRef(chain.get(idx - 1), catNames));
        }
        if (idx >= 0 && idx < chain.size() - 1) {
            vo.setNextArticle(toRef(chain.get(idx + 1), catNames));
        }
        return vo;
    }

    /**
     * 构建全学院阅读链：分类按 DFS 前序展开（sort ASC），分类内文章按 sort ASC, updated_at DESC。
     * <p>NULL 行为对齐 MySQL：sort ASC NULL 在前，updated_at DESC NULL 在后。</p>
     */
    private List<LearnArticleEntity> buildReadingChain() {
        List<LearnCategoryEntity> allCats = categoryMapper.selectList(null);
        if (allCats.isEmpty()) return List.of();
        List<LearnCategoryTreeVO> tree = buildTree(allCats);
        List<Long> orderedCatIds = new ArrayList<>();
        flattenTreeIds(tree, orderedCatIds);

        Map<Long, Integer> catOrder = new HashMap<>();
        for (int i = 0; i < orderedCatIds.size(); i++) {
            catOrder.put(orderedCatIds.get(i), i);
        }

        List<LearnArticleEntity> all = articleMapper.selectList(new QueryWrapper<LearnArticleEntity>()
                .eq("status", ArticleStatus.PUBLISHED.getCode()));
        all.sort(Comparator
                .comparingInt((LearnArticleEntity a) -> catOrder.getOrDefault(a.getCategoryId(), Integer.MAX_VALUE))
                .thenComparing(LearnArticleEntity::getSort, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(LearnArticleEntity::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return all;
    }

    private void flattenTreeIds(List<LearnCategoryTreeVO> nodes, List<Long> out) {
        if (nodes == null) return;
        for (LearnCategoryTreeVO n : nodes) {
            out.add(n.getId());
            flattenTreeIds(n.getChildren(), out);
        }
    }

    private Map<Long, String> loadCategoryNames() {
        List<LearnCategoryEntity> all = categoryMapper.selectList(null);
        Map<Long, String> map = new HashMap<>();
        for (LearnCategoryEntity c : all) map.put(c.getId(), c.getName());
        return map;
    }

    private LearnArticleRefVO toRef(LearnArticleEntity e, Map<Long, String> catNames) {
        LearnArticleRefVO r = new LearnArticleRefVO();
        r.setId(e.getId());
        r.setTitle(e.getTitle());
        r.setCategoryName(catNames.get(e.getCategoryId()));
        return r;
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
