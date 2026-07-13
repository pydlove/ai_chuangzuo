package com.aichuangzuo.user.modules.learn.service;

import com.aichuangzuo.user.modules.learn.entity.LearnArticleEntity;
import com.aichuangzuo.user.modules.learn.entity.LearnCategoryEntity;
import com.aichuangzuo.user.modules.learn.enums.ArticleStatus;
import com.aichuangzuo.user.modules.learn.enums.ContentType;
import com.aichuangzuo.user.modules.learn.mapper.LearnArticleMapper;
import com.aichuangzuo.user.modules.learn.mapper.LearnCategoryMapper;
import com.aichuangzuo.user.modules.learn.vo.LearnArticleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@Transactional
@Rollback
class LearnBrowseServiceImplTest {

    @Autowired
    private LearnBrowseService service;

    @Autowired
    private LearnCategoryMapper categoryMapper;

    @Autowired
    private LearnArticleMapper articleMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 清空创作学院相关表，隔离已种子数据。
     * 由 @Transactional + @Rollback 保证测试结束后自动回滚，不影响真实数据。
     */
    @BeforeEach
    void cleanTables() {
        jdbcTemplate.update("DELETE FROM t_article");
        jdbcTemplate.update("DELETE FROM t_article_category");
    }

    @Test
    void shouldChainArticlesAcrossCategories() {
        // C1 (sort=1) → A1, A2;  C2 (sort=2) → A3;  C3 (sort=3) → A4
        LearnCategoryEntity c1 = insertCategory(null, "C1", 1);
        LearnCategoryEntity c2 = insertCategory(null, "C2", 2);
        LearnCategoryEntity c3 = insertCategory(null, "C3", 3);

        LearnArticleEntity a1 = insertArticle(c1.getId(), "A1", 1, ArticleStatus.PUBLISHED);
        LearnArticleEntity a2 = insertArticle(c1.getId(), "A2", 2, ArticleStatus.PUBLISHED);
        LearnArticleEntity a3 = insertArticle(c2.getId(), "A3", 1, ArticleStatus.PUBLISHED);
        LearnArticleEntity a4 = insertArticle(c3.getId(), "A4", 1, ArticleStatus.PUBLISHED);

        // 期望链：A1 - A2 - A3 - A4
        LearnArticleVO v1 = service.articleDetail(a1.getId());
        assertNull(v1.getPrevArticle());
        assertNotNull(v1.getNextArticle());
        assertEquals(a2.getId(), v1.getNextArticle().getId());
        assertEquals("C1", v1.getNextArticle().getCategoryName());

        LearnArticleVO v2 = service.articleDetail(a2.getId());
        assertEquals(a1.getId(), v2.getPrevArticle().getId());
        assertEquals(a3.getId(), v2.getNextArticle().getId());
        assertEquals("C2", v2.getNextArticle().getCategoryName());

        LearnArticleVO v3 = service.articleDetail(a3.getId());
        assertEquals(a2.getId(), v3.getPrevArticle().getId());
        assertEquals(a4.getId(), v3.getNextArticle().getId());

        LearnArticleVO v4 = service.articleDetail(a4.getId());
        assertEquals(a3.getId(), v4.getPrevArticle().getId());
        assertNull(v4.getNextArticle());
    }

    @Test
    void shouldSkipDraftArticlesInChain() {
        LearnCategoryEntity c1 = insertCategory(null, "C1", 1);
        LearnArticleEntity a1 = insertArticle(c1.getId(), "A1", 1, ArticleStatus.PUBLISHED);
        insertArticle(c1.getId(), "DRAFT", 2, ArticleStatus.DRAFT);
        LearnArticleEntity a3 = insertArticle(c1.getId(), "A3", 3, ArticleStatus.PUBLISHED);

        LearnArticleVO v1 = service.articleDetail(a1.getId());
        assertEquals(a3.getId(), v1.getNextArticle().getId());

        LearnArticleVO v3 = service.articleDetail(a3.getId());
        assertEquals(a1.getId(), v3.getPrevArticle().getId());
    }

    @Test
    void shouldUseUpdatedAtDescWhenSortEquals() {
        LearnCategoryEntity c1 = insertCategory(null, "C1", 1);
        // 两篇 sort 相同，updated_at 新的在前（ORDER BY sort ASC, updated_at DESC）
        LearnArticleEntity older = insertArticle(c1.getId(), "Older", 1, ArticleStatus.PUBLISHED);
        LearnArticleEntity newer = insertArticle(c1.getId(), "Newer", 1, ArticleStatus.PUBLISHED);
        // 显式控制 updated_at（绕过 FieldFill.INSERT_UPDATE 的自动覆盖）
        forceUpdatedAt(older.getId(), LocalDateTime.now().minusDays(2));
        forceUpdatedAt(newer.getId(), LocalDateTime.now().minusDays(1));

        LearnArticleVO vNewer = service.articleDetail(newer.getId());
        assertNull(vNewer.getPrevArticle());
        assertNotNull(vNewer.getNextArticle());
        assertEquals(older.getId(), vNewer.getNextArticle().getId());
    }

    @Test
    void shouldRespectCategoryDfsOrder() {
        // C1 (sort=1) 含子分类 C1.1 (sort=1)，C2 (sort=2)
        // DFS 前序：C1 → C1.1 → C2
        LearnCategoryEntity c1 = insertCategory(null, "C1", 1);
        LearnCategoryEntity c11 = insertCategory(c1.getId(), "C1.1", 1);
        LearnCategoryEntity c2 = insertCategory(null, "C2", 2);

        LearnArticleEntity aC1 = insertArticle(c1.getId(), "aC1", 1, ArticleStatus.PUBLISHED);
        LearnArticleEntity aC11 = insertArticle(c11.getId(), "aC1.1", 1, ArticleStatus.PUBLISHED);
        LearnArticleEntity aC2 = insertArticle(c2.getId(), "aC2", 1, ArticleStatus.PUBLISHED);

        LearnArticleVO vC1 = service.articleDetail(aC1.getId());
        assertEquals(aC11.getId(), vC1.getNextArticle().getId());

        LearnArticleVO vC11 = service.articleDetail(aC11.getId());
        assertEquals(aC1.getId(), vC11.getPrevArticle().getId());
        assertEquals(aC2.getId(), vC11.getNextArticle().getId());
    }

    @Test
    void shouldReturnNullPrevAndNextWhenOnlyOneArticle() {
        LearnCategoryEntity c1 = insertCategory(null, "C1", 1);
        LearnArticleEntity only = insertArticle(c1.getId(), "Only", 1, ArticleStatus.PUBLISHED);

        LearnArticleVO vo = service.articleDetail(only.getId());
        assertNull(vo.getPrevArticle());
        assertNull(vo.getNextArticle());
    }

    // ---------- helpers ----------

    private LearnCategoryEntity insertCategory(Long parentId, String name, int sort) {
        LearnCategoryEntity e = new LearnCategoryEntity();
        e.setParentId(parentId);
        e.setName(name);
        e.setSort(sort);
        categoryMapper.insert(e);
        return e;
    }

    private LearnArticleEntity insertArticle(Long categoryId, String title, int sort, ArticleStatus status) {
        LearnArticleEntity e = new LearnArticleEntity();
        e.setCategoryId(categoryId);
        e.setTitle(title);
        e.setSummary("summary-" + title);
        e.setContentType(ContentType.MARKDOWN);
        e.setContent("# " + title);
        e.setStatus(status);
        e.setSort(sort);
        e.setPublishedAt(status == ArticleStatus.PUBLISHED ? LocalDateTime.now() : null);
        articleMapper.insert(e);
        return e;
    }

    /** 直接用 JdbcTemplate 更新 updated_at，绕过 MyBatis-Plus 的 FieldFill.INSERT_UPDATE。 */
    private void forceUpdatedAt(Long id, LocalDateTime updatedAt) {
        jdbcTemplate.update("UPDATE t_article SET updated_at = ? WHERE id = ?",
                Timestamp.valueOf(updatedAt), id);
    }
}
