package com.aichuangzuo.admin.modules.learn.service.impl;

import com.aichuangzuo.admin.modules.learn.dto.request.LearnArticleReq;
import com.aichuangzuo.admin.modules.learn.entity.LearnArticleEntity;
import com.aichuangzuo.admin.modules.learn.enums.ArticleStatus;
import com.aichuangzuo.admin.modules.learn.enums.ContentType;
import com.aichuangzuo.admin.modules.learn.exception.LearnErrorCode;
import com.aichuangzuo.admin.modules.learn.mapper.LearnArticleMapper;
import com.aichuangzuo.admin.modules.learn.mapper.LearnCategoryMapper;
import com.aichuangzuo.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearnArticleServiceImplTest {

    @Mock LearnArticleMapper articleMapper;
    @Mock LearnCategoryMapper categoryMapper;
    @InjectMocks LearnArticleServiceImpl service;

    @Test
    void publish_fromDraft_writesPublishedAt() {
        LearnArticleEntity draft = article(10L, ArticleStatus.DRAFT, null);
        when(articleMapper.selectById(10L)).thenReturn(draft);
        service.publish(10L);

        ArgumentCaptor<LearnArticleEntity> cap = ArgumentCaptor.forClass(LearnArticleEntity.class);
        verify(articleMapper).updateById(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(cap.getValue().getPublishedAt()).isNotNull();
    }

    @Test
    void publish_alreadyPublished_keepsOriginalPublishedAt() {
        LocalDateTime original = LocalDateTime.of(2026, 1, 1, 0, 0);
        LearnArticleEntity published = article(11L, ArticleStatus.PUBLISHED, original);
        when(articleMapper.selectById(11L)).thenReturn(published);
        service.publish(11L);

        ArgumentCaptor<LearnArticleEntity> cap = ArgumentCaptor.forClass(LearnArticleEntity.class);
        verify(articleMapper).updateById(cap.capture());
        assertThat(cap.getValue().getPublishedAt()).isEqualTo(original);
    }

    @Test
    void unpublish_keepsPublishedAt() {
        LocalDateTime original = LocalDateTime.of(2026, 2, 1, 0, 0);
        LearnArticleEntity published = article(12L, ArticleStatus.PUBLISHED, original);
        when(articleMapper.selectById(12L)).thenReturn(published);
        service.unpublish(12L);

        ArgumentCaptor<LearnArticleEntity> cap = ArgumentCaptor.forClass(LearnArticleEntity.class);
        verify(articleMapper).updateById(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(ArticleStatus.DRAFT);
        assertThat(cap.getValue().getPublishedAt()).isEqualTo(original);
    }

    @Test
    void update_publishedArticle_rejectsContentTypeChange() {
        LearnArticleEntity published = article(20L, ArticleStatus.PUBLISHED, LocalDateTime.now());
        published.setContentType(ContentType.MARKDOWN);
        when(articleMapper.selectById(20L)).thenReturn(published);

        LearnArticleReq req = new LearnArticleReq();
        req.setCategoryId(1L);
        req.setTitle("t");
        req.setContentType(ContentType.RICH_TEXT);
        req.setContent("html");

        assertThatThrownBy(() -> service.update(20L, req))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(LearnErrorCode.PUBLISHED_CONTENT_TYPE_LOCKED.getCode());
    }

    @Test
    void create_rejectsContentOverLimit() {
        when(categoryMapper.selectById(1L)).thenReturn(new com.aichuangzuo.admin.modules.learn.entity.LearnCategoryEntity());

        LearnArticleReq req = new LearnArticleReq();
        req.setCategoryId(1L);
        req.setTitle("t");
        req.setContentType(ContentType.MARKDOWN);
        req.setStatus(ArticleStatus.DRAFT);
        req.setContent("a".repeat(300_000));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(LearnErrorCode.CONTENT_TOO_LARGE.getCode());
        verify(articleMapper, never()).insert(any(LearnArticleEntity.class));
    }

    private LearnArticleEntity article(Long id, ArticleStatus status, LocalDateTime pub) {
        LearnArticleEntity e = new LearnArticleEntity();
        e.setId(id);
        e.setCategoryId(1L);
        e.setTitle("t");
        e.setStatus(status);
        e.setContentType(ContentType.MARKDOWN);
        e.setContent("x");
        e.setPublishedAt(pub);
        return e;
    }
}
