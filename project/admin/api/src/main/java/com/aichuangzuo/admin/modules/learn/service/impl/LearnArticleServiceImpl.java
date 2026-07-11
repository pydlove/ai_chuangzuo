package com.aichuangzuo.admin.modules.learn.service.impl;

import com.aichuangzuo.admin.modules.learn.dto.request.LearnArticlePageQuery;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnArticleReq;
import com.aichuangzuo.admin.modules.learn.dto.request.LearnSortReq;
import com.aichuangzuo.admin.modules.learn.entity.LearnArticleEntity;
import com.aichuangzuo.admin.modules.learn.enums.ArticleStatus;
import com.aichuangzuo.admin.modules.learn.enums.ContentType;
import com.aichuangzuo.admin.modules.learn.exception.LearnErrorCode;
import com.aichuangzuo.admin.modules.learn.mapper.LearnArticleMapper;
import com.aichuangzuo.admin.modules.learn.mapper.LearnCategoryMapper;
import com.aichuangzuo.admin.modules.learn.service.LearnArticleService;
import com.aichuangzuo.admin.modules.learn.vo.LearnArticleDetail;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LearnArticleServiceImpl implements LearnArticleService {

    private static final int MAX_MARKDOWN_BYTES = 200 * 1024;
    private static final int MAX_HTML_BYTES = 500 * 1024;

    private final LearnArticleMapper articleMapper;
    private final LearnCategoryMapper categoryMapper;

    @Override
    public IPage<LearnArticleDetail> page(LearnArticlePageQuery q) {
        Page<LearnArticleEntity> page = new Page<>(q.getPage(), q.getSize());
        QueryWrapper<LearnArticleEntity> qw = new QueryWrapper<>();
        if (q.getCategoryId() != null) qw.eq("category_id", q.getCategoryId());
        if (q.getStatus() != null && !q.getStatus().isBlank()) {
            try {
                qw.eq("status", ArticleStatus.fromCode(q.getStatus()).getCode());
            } catch (IllegalArgumentException ignored) {
                // 非法状态值：忽略该过滤条件
            }
        }
        if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
            qw.and(w -> w.like("title", q.getKeyword()).or().like("summary", q.getKeyword()));
        }
        qw.orderByDesc("updated_at");
        Page<LearnArticleEntity> res = articleMapper.selectPage(page, qw);
        return res.convert(this::toVo);
    }

    @Override
    public LearnArticleDetail detail(Long id) {
        LearnArticleEntity e = requireExisting(id);
        return toVo(e);
    }

    @Override
    public Long create(LearnArticleReq req) {
        requireCategoryExists(req.getCategoryId());
        validateContentSize(req.getContentType(), req.getContent());
        LearnArticleEntity e = new LearnArticleEntity();
        copyFromReq(e, req);
        // 初次创建：写入状态；published_at 在 status 已是 published 且发布时间未写入时填 now
        applyStatusTransition(e, null);
        articleMapper.insert(e);
        return e.getId();
    }

    @Override
    public void update(Long id, LearnArticleReq req) {
        LearnArticleEntity exist = requireExisting(id);
        // 已发布文章不允许切换正文类型
        if (exist.getStatus() == ArticleStatus.PUBLISHED
                && !exist.getContentType().equals(req.getContentType())) {
            throw new BusinessException(LearnErrorCode.PUBLISHED_CONTENT_TYPE_LOCKED);
        }
        requireCategoryExists(req.getCategoryId());
        validateContentSize(req.getContentType(), req.getContent());
        // 不允许通过 update 改 status（状态机由 publish/unpublish 显式控制）
        LearnArticleEntity beforeStatus = new LearnArticleEntity();
        ArticleStatus originalStatus = exist.getStatus();
        copyFromReq(exist, req);
        exist.setStatus(originalStatus);
        articleMapper.updateById(exist);
    }

    @Override
    public void delete(Long id) {
        requireExisting(id);
        articleMapper.deleteById(id);
    }

    @Override
    public void publish(Long id) {
        LearnArticleEntity e = requireExisting(id);
        applyStatusTransition(e, ArticleStatus.PUBLISHED);
        articleMapper.updateById(e);
    }

    @Override
    public void unpublish(Long id) {
        LearnArticleEntity e = requireExisting(id);
        applyStatusTransition(e, ArticleStatus.DRAFT);
        articleMapper.updateById(e);
    }

    @Override
    public void move(Long id, Long categoryId) {
        requireCategoryExists(categoryId);
        LearnArticleEntity e = requireExisting(id);
        e.setCategoryId(categoryId);
        articleMapper.updateById(e);
    }

    @Override
    public void sortBatch(List<LearnSortReq.SortItem> items) {
        for (LearnSortReq.SortItem it : items) {
            LearnArticleEntity e = requireExisting(it.getId());
            e.setSort(it.getSort());
            articleMapper.updateById(e);
        }
    }

    // -------- helpers --------

    private void applyStatusTransition(LearnArticleEntity e, ArticleStatus target) {
        if (target != null) e.setStatus(target);
        // 草稿 → 已发布：写入 published_at
        // 已发布编辑：不刷 published_at（保持首次发布时间）
        if (e.getStatus() == ArticleStatus.PUBLISHED && e.getPublishedAt() == null) {
            e.setPublishedAt(LocalDateTime.now());
        }
        // 发布 → 草稿：published_at 保留
    }

    private void copyFromReq(LearnArticleEntity e, LearnArticleReq req) {
        e.setCategoryId(req.getCategoryId());
        e.setTitle(req.getTitle());
        e.setSummary(req.getSummary());
        e.setContentType(req.getContentType());
        e.setContent(req.getContent());
        if (req.getSort() != null) e.setSort(req.getSort());
        // 不通过 copyFromReq 设 status — 由显式 publish/unpublish 控制
    }

    private void validateContentSize(ContentType type, String content) {
        int bytes = content == null ? 0 : content.getBytes().length;
        int max = type == ContentType.MARKDOWN ? MAX_MARKDOWN_BYTES : MAX_HTML_BYTES;
        if (bytes > max) {
            throw new BusinessException(LearnErrorCode.CONTENT_TOO_LARGE);
        }
    }

    private LearnArticleEntity requireExisting(Long id) {
        LearnArticleEntity e = articleMapper.selectById(id);
        if (e == null) {
            throw new BusinessException(LearnErrorCode.ARTICLE_NOT_FOUND);
        }
        return e;
    }

    private void requireCategoryExists(Long id) {
        if (id == null || categoryMapper.selectById(id) == null) {
            throw new BusinessException(LearnErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    private LearnArticleDetail toVo(LearnArticleEntity e) {
        LearnArticleDetail v = new LearnArticleDetail();
        v.setId(e.getId());
        v.setCategoryId(e.getCategoryId());
        v.setTitle(e.getTitle());
        v.setSummary(e.getSummary());
        v.setContentType(e.getContentType());
        v.setContent(e.getContent());
        v.setStatus(e.getStatus());
        v.setSort(e.getSort());
        v.setAuthorId(e.getAuthorId());
        v.setPublishedAt(e.getPublishedAt());
        v.setCreatedAt(e.getCreatedAt());
        v.setUpdatedAt(e.getUpdatedAt());
        return v;
    }
}
