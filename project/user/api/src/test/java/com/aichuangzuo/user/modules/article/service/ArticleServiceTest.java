package com.aichuangzuo.user.modules.article.service;

import com.aichuangzuo.shared.exception.BusinessException;
import com.aichuangzuo.user.modules.article.dto.request.SaveArticleRequest;
import com.aichuangzuo.user.modules.article.dto.request.UpdateArticleRequest;
import com.aichuangzuo.user.modules.article.entity.Article;
import com.aichuangzuo.user.modules.article.mapper.ArticleMapper;
import com.aichuangzuo.user.modules.article.vo.ArticlePageVO;
import com.aichuangzuo.user.modules.article.vo.ArticleVO;
import com.aichuangzuo.user.modules.auth.entity.User;
import com.aichuangzuo.user.modules.auth.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@Rollback
class ArticleServiceTest {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void save_shouldInsertArticleAndReturnBizNo() {
        User user = createUser("article-save@test.com");

        SaveArticleRequest request = new SaveArticleRequest();
        request.setTitle("  我的第一篇作品  ");
        request.setBody("正文内容");
        request.setPlatform("xiaohongshu");
        request.setStyle("warm");
        request.setTemplate("card-01");
        request.setWordCount(120);

        String bizNo = articleService.save(user.getId(), request);

        assertNotNull(bizNo);
        assertTrue(bizNo.startsWith("A"));
        Article stored = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getBizNo, bizNo));
        assertNotNull(stored);
        assertEquals(user.getId(), stored.getUserId());
        assertEquals("我的第一篇作品", stored.getTitle());
        assertEquals("正文内容", stored.getBody());
        assertEquals("xiaohongshu", stored.getPlatform());
        assertEquals(Integer.valueOf(120), stored.getWordCount());
    }

    @Test
    void save_shouldThrowWhenTitleBlank() {
        User user = createUser("article-empty-title@test.com");
        SaveArticleRequest request = new SaveArticleRequest();
        request.setTitle("   ");
        request.setBody("正文");
        assertThrows(BusinessException.class, () -> articleService.save(user.getId(), request));
    }

    @Test
    void save_shouldThrowWhenBodyBlank() {
        User user = createUser("article-empty-body@test.com");
        SaveArticleRequest request = new SaveArticleRequest();
        request.setTitle("标题");
        request.setBody("");
        assertThrows(BusinessException.class, () -> articleService.save(user.getId(), request));
    }

    @Test
    void save_shouldNormalizeInvalidStyleOverridesToDefault() {
        User user = createUser("article-overrides-bad@test.com");
        SaveArticleRequest request = new SaveArticleRequest();
        request.setTitle("title");
        request.setBody("body");
        request.setStyleOverrides("not-json");

        String bizNo = articleService.save(user.getId(), request);

        Article stored = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getBizNo, bizNo));
        // MySQL JSON 列会规范化空白
        assertEquals("{\"blocks\": {}, \"inlines\": []}", stored.getStyleOverrides());
    }

    @Test
    void list_shouldReturnCurrentUserOnlyOrderedByCompletedAtDesc() {
        User alice = createUser("article-list-alice@test.com");
        User bob = createUser("article-list-bob@test.com");
        String bizNoA1 = createArticle(alice.getId(), "A1", "b", "2026-01-01T10:00:00");
        createArticle(alice.getId(), "A2", "b", "2026-01-02T10:00:00");
        createArticle(bob.getId(), "B1", "b", "2026-01-03T10:00:00");

        ArticlePageVO page = articleService.list(alice.getId(), null, 1, 10);

        assertEquals(2, page.getTotal());
        assertEquals(2, page.getList().size());
        assertEquals("A2", page.getList().get(0).getTitle());
        assertEquals(bizNoA1, page.getList().get(1).getBizNo());
    }

    @Test
    void list_shouldFilterByKeyword() {
        User user = createUser("article-list-kw@test.com");
        createArticle(user.getId(), "春天踏青", "b", "2026-01-01T10:00:00");
        createArticle(user.getId(), "夏天海边", "b", "2026-01-02T10:00:00");

        ArticlePageVO page = articleService.list(user.getId(), "夏天", 1, 10);

        assertEquals(1, page.getTotal());
        assertEquals("夏天海边", page.getList().get(0).getTitle());
    }

    @Test
    void get_shouldReturnCurrentUserArticle() {
        User user = createUser("article-get@test.com");
        String bizNo = createArticle(user.getId(), "get 测试", "正文", "2026-02-01T10:00:00");

        ArticleVO vo = articleService.get(user.getId(), bizNo);

        assertEquals(bizNo, vo.getBizNo());
        assertEquals("get 测试", vo.getTitle());
    }

    @Test
    void get_shouldThrowWhenNotFound() {
        User user = createUser("article-get-missing@test.com");
        assertThrows(BusinessException.class, () -> articleService.get(user.getId(), "A0000000000000000"));
    }

    @Test
    void get_shouldThrowForOtherUsersArticle() {
        User alice = createUser("article-get-isolation-alice@test.com");
        User bob = createUser("article-get-isolation-bob@test.com");
        String bizNo = createArticle(alice.getId(), "私密作品", "b", "2026-02-01T10:00:00");
        assertThrows(BusinessException.class, () -> articleService.get(bob.getId(), bizNo));
    }

    @Test
    void update_shouldUpdateTitleAndBody() {
        User user = createUser("article-update@test.com");
        String bizNo = createArticle(user.getId(), "原标题", "原正文", "2026-02-01T10:00:00");

        UpdateArticleRequest request = new UpdateArticleRequest();
        request.setTitle("新标题");
        request.setBody("新正文");
        request.setStyleOverrides("{\"blocks\":{\"b1\":{}},\"inlines\":[]}");

        articleService.update(user.getId(), bizNo, request);

        Article stored = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getBizNo, bizNo));
        assertEquals("新标题", stored.getTitle());
        assertEquals("新正文", stored.getBody());
        // MySQL JSON 列会规范化空白
        assertEquals("{\"blocks\": {\"b1\": {}}, \"inlines\": []}", stored.getStyleOverrides());
    }

    @Test
    void update_shouldBeNoopWhenAllFieldsBlank() {
        User user = createUser("article-update-noop@test.com");
        String bizNo = createArticle(user.getId(), "原标题", "原正文", "2026-02-01T10:00:00");
        UpdateArticleRequest request = new UpdateArticleRequest();

        articleService.update(user.getId(), bizNo, request);

        Article stored = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getBizNo, bizNo));
        assertEquals("原标题", stored.getTitle());
        assertEquals("原正文", stored.getBody());
    }

    @Test
    void update_shouldThrowForOtherUsersArticle() {
        User alice = createUser("article-update-isolation-alice@test.com");
        User bob = createUser("article-update-isolation-bob@test.com");
        String bizNo = createArticle(alice.getId(), "私密", "b", "2026-02-01T10:00:00");
        UpdateArticleRequest request = new UpdateArticleRequest();
        request.setTitle("尝试改");
        assertThrows(BusinessException.class, () -> articleService.update(bob.getId(), bizNo, request));
    }

    @Test
    void delete_shouldSoftDelete() {
        User user = createUser("article-delete@test.com");
        String bizNo = createArticle(user.getId(), "t", "b", "2026-02-01T10:00:00");

        articleService.delete(user.getId(), bizNo);

        Article stored = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getBizNo, bizNo));
        assertNull(stored);
        // 软删后默认查询应当看不见
        ArticlePageVO page = articleService.list(user.getId(), null, 1, 10);
        assertEquals(0, page.getTotal());
    }

    @Test
    void delete_shouldThrowForOtherUsersArticle() {
        User alice = createUser("article-delete-isolation-alice@test.com");
        User bob = createUser("article-delete-isolation-bob@test.com");
        String bizNo = createArticle(alice.getId(), "私密", "b", "2026-02-01T10:00:00");
        assertThrows(BusinessException.class, () -> articleService.delete(bob.getId(), bizNo));
    }

    @Test
    void get_shouldExposeStyleOverridesAsObject() {
        User user = createUser("article-overrides-obj@test.com");
        String bizNo = createArticle(user.getId(), "t", "b", "2026-02-01T10:00:00");
        Article stored = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getBizNo, bizNo));
        stored.setStyleOverrides("{\"blocks\":{\"x\":1},\"inlines\":[\"a\"]}");
        articleMapper.updateById(stored);

        ArticleVO vo = articleService.get(user.getId(), bizNo);

        assertNotNull(vo.getStyleOverrides());
        assertTrue(vo.getStyleOverrides() instanceof java.util.Map);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> map = (java.util.Map<String, Object>) vo.getStyleOverrides();
        assertTrue(map.containsKey("blocks"));
        assertFalse(map.get("inlines") == null);
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

    private String createArticle(Long userId, String title, String body, String completedAt) {
        SaveArticleRequest request = new SaveArticleRequest();
        request.setTitle(title);
        request.setBody(body);
        request.setPlatform("xiaohongshu");
        request.setStyle("warm");
        request.setTemplate("card-01");
        request.setWordCount(100);
        request.setCompletedAt(java.time.LocalDateTime.parse(completedAt));
        return articleService.save(userId, request);
    }
}