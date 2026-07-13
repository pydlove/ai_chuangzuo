package com.aichuangzuo.user.modules.article.service;

import com.aichuangzuo.user.modules.article.entity.Article;
import com.aichuangzuo.user.modules.article.mapper.ArticleMapper;
import com.aichuangzuo.user.modules.article.service.impl.ArticleServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceMonthlyCountTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ArticleServiceImpl articleService;

    @Test
    void monthlyCount_returnsMapperCount() {
        Long userId = 100L;
        when(articleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        Long count = articleService.monthlyCount(userId);

        assertEquals(3L, count);
    }
}
