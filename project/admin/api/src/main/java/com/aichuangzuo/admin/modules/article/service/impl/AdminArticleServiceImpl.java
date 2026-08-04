package com.aichuangzuo.admin.modules.article.service.impl;

import com.aichuangzuo.admin.modules.article.entity.Article;
import com.aichuangzuo.admin.modules.article.mapper.ArticleMapper;
import com.aichuangzuo.admin.modules.article.service.AdminArticleService;
import com.aichuangzuo.admin.modules.article.vo.AdminArticleDetailVO;
import com.aichuangzuo.admin.modules.article.vo.AdminArticlePageVO;
import com.aichuangzuo.admin.modules.article.vo.AdminArticleVO;
import com.aichuangzuo.shared.enums.error.AdminUserErrorCode;
import com.aichuangzuo.shared.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端用户作品服务实现。
 */
@Service
@RequiredArgsConstructor
public class AdminArticleServiceImpl implements AdminArticleService {

    private final ArticleMapper articleMapper;

    @Override
    public AdminArticlePageVO listUserArticles(Long userId, String keyword, int page, int pageSize) {
        Page<Article> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getUserId, userId)
                .eq(Article::getIsDeleted, 0);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Article::getTitle, kw)
                    .or()
                    .like(Article::getDescription, kw));
        }
        wrapper.orderByDesc(Article::getCreatedAt);
        Page<Article> result = articleMapper.selectPage(pageParam, wrapper);

        List<AdminArticleVO> list = result.getRecords().stream()
                .map(this::toListVO)
                .collect(Collectors.toList());

        AdminArticlePageVO vo = new AdminArticlePageVO();
        vo.setList(list);
        vo.setTotal(result.getTotal());
        return vo;
    }

    @Override
    public AdminArticleDetailVO getArticleDetail(String bizNo) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getBizNo, bizNo)
                .eq(Article::getIsDeleted, 0);
        Article article = articleMapper.selectOne(wrapper);
        if (article == null) {
            throw new BusinessException(AdminUserErrorCode.USER_NOT_FOUND);
        }
        return toDetailVO(article);
    }

    private AdminArticleVO toListVO(Article article) {
        AdminArticleVO vo = new AdminArticleVO();
        vo.setBizNo(article.getBizNo());
        vo.setUserId(article.getUserId());
        vo.setTitle(article.getTitle());
        vo.setDescription(article.getDescription());
        vo.setPlatform(article.getPlatform());
        vo.setSkill(article.getSkill());
        vo.setTemplate(article.getTemplate());
        vo.setWordCount(article.getWordCount());
        vo.setCompletedAt(article.getCompletedAt());
        vo.setCreatedAt(article.getCreatedAt());
        return vo;
    }

    private AdminArticleDetailVO toDetailVO(Article article) {
        AdminArticleDetailVO vo = new AdminArticleDetailVO();
        vo.setBizNo(article.getBizNo());
        vo.setUserId(article.getUserId());
        vo.setTitle(article.getTitle());
        vo.setBody(article.getBody());
        vo.setDescription(article.getDescription());
        vo.setPlatform(article.getPlatform());
        vo.setSkill(article.getSkill());
        vo.setTemplate(article.getTemplate());
        vo.setWordCount(article.getWordCount());
        vo.setTagsJson(article.getTagsJson());
        vo.setStyleOverrides(article.getStyleOverrides());
        vo.setCompletedAt(article.getCompletedAt());
        vo.setCreatedAt(article.getCreatedAt());
        vo.setUpdatedAt(article.getUpdatedAt());
        return vo;
    }
}
