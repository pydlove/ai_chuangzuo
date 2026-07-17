package com.aichuangzuo.user.modules.article.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.article.entity.Article;
import com.aichuangzuo.user.modules.article.mapper.ArticleMapper;
import com.aichuangzuo.user.modules.article.vo.TitleOptimizeVO;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.aichuangzuo.user.modules.benefit.service.BenefitService;
import com.aichuangzuo.user.modules.benefit.vo.BenefitCheckVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@Rollback
class TitleOptimizeServiceTest {

    private static final String AI_JSON = """
            {"titles":{
              "wechat":["公众号标题一","公众号标题二"],
              "xiaohongshu":["小红书标题一","小红书标题二"],
              "toutiao":["头条标题一","头条标题二"],
              "baijiahao":["百家号标题一","百家号标题二"],
              "zhihu":["知乎标题一","知乎标题二"],
              "douyin":["抖音标题一","抖音标题二"],
              "bilibili":["B站标题一","B站标题二"]
            }}""";

    @Autowired
    private TitleOptimizeService titleOptimizeService;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @MockBean
    private BenefitService benefitService;

    @MockBean
    private TitleOptimizeAiService titleOptimizeAiService;

    @Test
    void optimize_shouldDenyWhenBenefitNotAllowed() {
        User user = createUser("title-opt-deny@test.com");
        String bizNo = createArticle(user.getId());
        when(benefitService.check(user.getId(), "ai_title_optimize")).thenReturn(benefitVo(false));

        assertThrows(BusinessException.class, () -> titleOptimizeService.optimize(user.getId(), bizNo));
        verify(titleOptimizeAiService, never()).call(anyString(), anyString());
    }

    @Test
    void optimize_shouldGenerateAndPersistOnFirstCallThenReturnCache() {
        User user = createUser("title-opt-cache@test.com");
        String bizNo = createArticle(user.getId());
        when(benefitService.check(user.getId(), "ai_title_optimize")).thenReturn(benefitVo(true));
        when(titleOptimizeAiService.call(anyString(), anyString())).thenReturn(AI_JSON);

        TitleOptimizeVO first = titleOptimizeService.optimize(user.getId(), bizNo);

        assertFalse(first.getCached());
        assertEquals(7, first.getTitles().size());
        assertEquals(List.of("B站标题一", "B站标题二"), first.getTitles().get("bilibili"));
        assertEquals(List.of("抖音标题一", "抖音标题二"), first.getTitles().get("douyin"));
        verify(titleOptimizeAiService, times(1)).call(anyString(), anyString());

        // 落库
        Article stored = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getBizNo, bizNo));
        assertNotNull(stored.getOptimizedTitlesJson());

        // 第二次：命中缓存，不再调大模型
        TitleOptimizeVO second = titleOptimizeService.optimize(user.getId(), bizNo);
        assertTrue(second.getCached());
        assertEquals(first.getTitles(), second.getTitles());
        verify(titleOptimizeAiService, times(1)).call(anyString(), anyString());
    }

    @Test
    void optimize_shouldThrowWhenAiOutputUnparseable() {
        User user = createUser("title-opt-badjson@test.com");
        String bizNo = createArticle(user.getId());
        when(benefitService.check(user.getId(), "ai_title_optimize")).thenReturn(benefitVo(true));
        when(titleOptimizeAiService.call(anyString(), anyString())).thenReturn("这不是 JSON");

        assertThrows(BusinessException.class, () -> titleOptimizeService.optimize(user.getId(), bizNo));

        // 失败不落库，下次仍可重试
        Article stored = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getBizNo, bizNo));
        assertEquals(null, stored.getOptimizedTitlesJson());
    }

    @Test
    void optimize_shouldThrowForOtherUsersArticle() {
        User alice = createUser("title-opt-iso-alice@test.com");
        User bob = createUser("title-opt-iso-bob@test.com");
        String bizNo = createArticle(alice.getId());
        when(benefitService.check(eq(bob.getId()), eq("ai_title_optimize"))).thenReturn(benefitVo(true));

        assertThrows(BusinessException.class, () -> titleOptimizeService.optimize(bob.getId(), bizNo));
        verify(titleOptimizeAiService, never()).call(anyString(), anyString());
    }

    private BenefitCheckVO benefitVo(boolean allowed) {
        BenefitCheckVO vo = new BenefitCheckVO();
        vo.setAllowed(allowed);
        vo.setCode("ai_title_optimize");
        vo.setType("boolean");
        vo.setValue(allowed ? "true" : "false");
        return vo;
    }

    private User createUser(String email) {
        User user = new User();
        user.setBizNo("B" + System.nanoTime());
        user.setEmail(email);
        user.setPasswordHash("x");
        user.setInviteCode("X" + System.nanoTime());
        user.setUserStatus(1);
        user.setEmailVerified(1);
        userMapper.insert(user);
        return user;
    }

    private String createArticle(Long userId) {
        Article article = new Article();
        article.setBizNo("A" + System.nanoTime());
        article.setUserId(userId);
        article.setTitle("时间管理的几点思考");
        article.setBody("正文内容");
        article.setWordCount(100);
        article.setCompletedAt(LocalDateTime.now());
        articleMapper.insert(article);
        return article.getBizNo();
    }
}
