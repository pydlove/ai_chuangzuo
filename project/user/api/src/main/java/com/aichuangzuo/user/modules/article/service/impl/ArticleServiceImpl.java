package com.aichuangzuo.user.modules.article.service.impl;

import com.aichuangzuo.user.modules.article.dto.request.SaveArticleRequest;
import com.aichuangzuo.user.modules.article.dto.request.UpdateArticleRequest;
import com.aichuangzuo.user.modules.article.entity.Article;
import com.aichuangzuo.user.modules.article.enums.ArticleErrorCode;
import com.aichuangzuo.user.modules.article.mapper.ArticleMapper;
import com.aichuangzuo.user.modules.article.service.ArticleService;
import com.aichuangzuo.user.modules.article.vo.ArticlePageVO;
import com.aichuangzuo.user.modules.article.vo.ArticleVO;
import com.aichuangzuo.user.modules.style.market.entity.StyleMarket;
import com.aichuangzuo.user.modules.style.market.mapper.StyleMarketMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aichuangzuo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户作品服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private static final String DEFAULT_STYLE_OVERRIDES_JSON = "{\"blocks\":{},\"inlines\":[]}";

    private final ArticleMapper articleMapper;
    private final ObjectMapper objectMapper;
    private final StyleMarketMapper styleMarketMapper;

    @Override
    public ArticlePageVO list(Long userId, String keyword, long page, long pageSize) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getUserId, userId)
                .eq(Article::getIsDeleted, 0)
                .orderByDesc(Article::getCompletedAt)
                .orderByDesc(Article::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Article::getTitle, keyword.trim());
        }
        IPage<Article> result = articleMapper.selectPage(new Page<>(page, pageSize), wrapper);
        ArticlePageVO vo = new ArticlePageVO();
        vo.setList(result.getRecords().stream().map(this::toVo).collect(Collectors.toList()));
        vo.setTotal(result.getTotal());
        vo.setPage(result.getCurrent());
        vo.setPageSize(result.getSize());
        return vo;
    }

    @Override
    public ArticleVO get(Long userId, String bizNo) {
        Article article = mustFind(userId, bizNo);
        return toVo(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String save(Long userId, SaveArticleRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BusinessException(ArticleErrorCode.ARTICLE_TITLE_EMPTY);
        }
        if (!StringUtils.hasText(request.getBody())) {
            throw new BusinessException(ArticleErrorCode.ARTICLE_BODY_EMPTY);
        }
        Article article = new Article();
        article.setBizNo(generateBizNo());
        article.setUserId(userId);
        article.setTitle(request.getTitle().trim());
        article.setBody(request.getBody());
        article.setStyleOverrides(normalizeStyleOverrides(request.getStyleOverrides()));
        article.setPlatform(request.getPlatform());
        article.setStyle(request.getStyle());
        article.setTemplate(request.getTemplate());
        article.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        article.setTagsJson(toTagsJson(request.getTags()));
        article.setWordCount(request.getWordCount() == null ? 0 : Math.max(0, request.getWordCount()));
        article.setCompletedAt(request.getCompletedAt() != null ? request.getCompletedAt() : LocalDateTime.now());
        articleMapper.insert(article);
        log.info("保存作品完成 userId={}, bizNo={}, title={}", userId, article.getBizNo(), article.getTitle());
        return article.getBizNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, String bizNo, UpdateArticleRequest request) {
        mustFind(userId, bizNo);
        LambdaUpdateWrapper<Article> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Article::getUserId, userId)
                .eq(Article::getBizNo, bizNo)
                .eq(Article::getIsDeleted, 0);
        boolean touched = false;
        if (StringUtils.hasText(request.getTitle())) {
            wrapper.set(Article::getTitle, request.getTitle().trim());
            touched = true;
        }
        if (StringUtils.hasText(request.getBody())) {
            wrapper.set(Article::getBody, request.getBody());
            touched = true;
        }
        if (request.getStyleOverrides() != null) {
            wrapper.set(Article::getStyleOverrides, normalizeStyleOverrides(request.getStyleOverrides()));
            touched = true;
        }
        if (!touched) {
            return;
        }
        articleMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, String bizNo) {
        mustFind(userId, bizNo);
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getUserId, userId)
                .eq(Article::getBizNo, bizNo)
                .eq(Article::getIsDeleted, 0)
                .set(Article::getIsDeleted, 1));
    }

    private Article mustFind(Long userId, String bizNo) {
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getUserId, userId)
                .eq(Article::getBizNo, bizNo)
                .eq(Article::getIsDeleted, 0));
        if (article == null) {
            throw new BusinessException(ArticleErrorCode.ARTICLE_NOT_FOUND);
        }
        return article;
    }

    private ArticleVO toVo(Article article) {
        ArticleVO vo = new ArticleVO();
        vo.setBizNo(article.getBizNo());
        vo.setTitle(article.getTitle());
        vo.setBody(article.getBody());
        vo.setStyleOverrides(parseStyleOverrides(article.getStyleOverrides()));
        vo.setPlatform(article.getPlatform());
        vo.setStyle(article.getStyle());
        vo.setStyleName(resolveStyleName(article.getStyle()));
        vo.setTemplate(article.getTemplate());
        vo.setDescription(article.getDescription());
        vo.setTags(parseTags(article.getTagsJson()));
        vo.setWordCount(article.getWordCount());
        vo.setCompletedAt(article.getCompletedAt());
        vo.setCreatedAt(article.getCreatedAt());
        vo.setUpdatedAt(article.getUpdatedAt());
        return vo;
    }

    /**
     * 解析风格可读名称。
     * <p>市场风格以 SM 开头，需查 u_style_market.biz_no 获取 style_name；
     * 其余情况 style 字段本身即为名称（用户自定义/学习/系统预设风格名）。
     */
    private String resolveStyleName(String style) {
        if (!StringUtils.hasText(style)) {
            return null;
        }
        if (style.startsWith("SM")) {
            StyleMarket market = styleMarketMapper.selectOne(
                    new LambdaQueryWrapper<StyleMarket>()
                            .eq(StyleMarket::getBizNo, style)
                            .eq(StyleMarket::getIsDeleted, 0)
                            .last("LIMIT 1"));
            return market != null && StringUtils.hasText(market.getStyleName())
                    ? market.getStyleName()
                    : style;
        }
        return style;
    }

    private String normalizeStyleOverrides(String raw) {
        if (!StringUtils.hasText(raw)) {
            return DEFAULT_STYLE_OVERRIDES_JSON;
        }
        try {
            objectMapper.readTree(raw);
            return raw;
        } catch (JsonProcessingException e) {
            return DEFAULT_STYLE_OVERRIDES_JSON;
        }
    }

    private Object parseStyleOverrides(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, Object.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String toTagsJson(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<String> parseTags(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public Long monthlyCount(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusMonths(1);
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getUserId, userId)
                .ge(Article::getCompletedAt, start)
                .lt(Article::getCompletedAt, end);
        return articleMapper.selectCount(wrapper);
    }

    private String generateBizNo() {
        return "A" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}