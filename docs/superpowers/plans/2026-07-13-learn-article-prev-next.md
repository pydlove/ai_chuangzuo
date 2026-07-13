# 创作学院文章上一篇 / 下一篇导流 实施方案

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在创作学院文章详情页底部增加「上一篇 / 下一篇」导航，跨分类自动衔接，把整所学院拉通成一条阅读链。

**Architecture:** 后端在 `LearnBrowseServiceImpl.articleDetail()` 里一次性查出所有已发布文章，内存里按"分类 DFS 前序 + 分类内 sort ASC, updated_at DESC"排序后取邻居，挂在 `LearnArticleVO` 新增的 `prevArticle` / `nextArticle` 字段上返回。前端 `LearnContent.vue` 在正文与 CTA 之间渲染两张导航卡片，首/末篇对应方向不渲染；`LearnIndex.vue` 顺手修切换文章不滚顶部的 bug。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Lombok（后端），Vue 3 + Vue Router 4 + Ant Design Vue（前端），JUnit 5 + @SpringBootTest（后端测试），Playwright Python（e2e）。

## Global Constraints

- 不新增任何接口，只改 `GET /api/v1/user/learn/article/{id}` 的响应体。
- 分类树接口 `/learn/category/tree` 与分类详情接口 `/learn/category/{id}` **不改动**。
- 文章排序规则必须严格复用 `categoryDetail` 已有的 `sort ASC, updated_at DESC`，不能引入新规则。
- 首篇 `prevArticle` 为 `null`，末篇 `nextArticle` 为 `null`；整个 `learn-nav` 容器在两者都为 `null` 时不渲染。
- 跨分类时按钮内显示 `《分类名》`；同分类不显示。
- 移动端（`< 992px`）两张卡片上下堆叠。
- 后端测试沿用现有约定：`@SpringBootTest + @Transactional + @Rollback`，操作真实 MySQL 然后回滚。
- 提交拆分：后端一个 commit，前端一个 commit，e2e 一个 commit。

---

## 文件结构

| 操作 | 路径 | 责任 |
|---|---|---|
| 新建 | `project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/vo/LearnArticleRefVO.java` | 上下篇引用 VO（id/title/categoryName） |
| 修改 | `project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/vo/LearnArticleVO.java` | 新增 `prevArticle` / `nextArticle` 字段 |
| 修改 | `project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/service/impl/LearnBrowseServiceImpl.java` | `articleDetail` 内构建阅读链并填充 prev/next |
| 新建 | `project/user/api/src/test/java/com/aichuangzuo/user/modules/learn/service/LearnBrowseServiceImplTest.java` | 后端单测：跨分类阅读链正确性 |
| 修改 | `project/user/web/src/components/learn/LearnContent.vue` | 底部新增上下篇导航卡片 |
| 修改 | `project/user/web/src/views/LearnIndex.vue` | 计算并下传 `currentCategoryName`；切文章滚顶部 |
| 新建 | `tests/e2e/learn_article_nav.py` | e2e 验证首/中/末/跨分类/移动端 |

---

### Task 1: 后端 - 文章详情接口返回 prev/next

**Files:**
- Create: `project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/vo/LearnArticleRefVO.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/vo/LearnArticleVO.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/service/impl/LearnBrowseServiceImpl.java`
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/modules/learn/service/LearnBrowseServiceImplTest.java`

**Interfaces:**
- Consumes: 现有 `LearnArticleMapper.selectList/selectOne`、`LearnCategoryMapper.selectList/selectById`、`buildTree` 私有方法（同文件）。
- Produces:
  - `LearnArticleRefVO { Long id; String title; String categoryName; }`
  - `LearnArticleVO.prevArticle : LearnArticleRefVO`（首篇为 `null`）
  - `LearnArticleVO.nextArticle : LearnArticleRefVO`（末篇为 `null`）
  - 私有方法 `buildReadingChain(): List<LearnArticleEntity>`
  - 私有方法 `flattenTreeIds(List<LearnCategoryTreeVO>): List<Long>`
  - 私有方法 `toRef(LearnArticleEntity, Map<Long, String>): LearnArticleRefVO`

- [ ] **Step 1: 新建 `LearnArticleRefVO.java`**

```java
package com.aichuangzuo.user.modules.learn.vo;

import lombok.Data;

/**
 * 创作学院 - 文章上一篇/下一篇引用 VO。
 * <p>用于文章详情底部的阅读链导航。</p>
 */
@Data
public class LearnArticleRefVO {
    private Long id;
    private String title;
    private String categoryName;
}
```

- [ ] **Step 2: 修改 `LearnArticleVO.java` 增加两个字段**

把现有 `LearnArticleVO` 改为：

```java
package com.aichuangzuo.user.modules.learn.vo;

import com.aichuangzuo.user.modules.learn.enums.ContentType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LearnArticleVO {
    private Long id;
    private Long categoryId;
    private String title;
    private String summary;
    private ContentType contentType;
    private String content;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
    /** 上一篇，首篇为 null。 */
    private LearnArticleRefVO prevArticle;
    /** 下一篇，末篇为 null。 */
    private LearnArticleRefVO nextArticle;
}
```

- [ ] **Step 3: 编写失败测试**

新建 `LearnBrowseServiceImplTest.java`：

```java
package com.aichuangzuo.user.modules.learn.service;

import com.aichuangzuo.user.modules.learn.entity.LearnArticleEntity;
import com.aichuangzuo.user.modules.learn.entity.LearnCategoryEntity;
import com.aichuangzuo.user.modules.learn.enums.ArticleStatus;
import com.aichuangzuo.user.modules.learn.enums.ContentType;
import com.aichuangzuo.user.modules.learn.mapper.LearnArticleMapper;
import com.aichuangzuo.user.modules.learn.mapper.LearnCategoryMapper;
import com.aichuangzuo.user.modules.learn.vo.LearnArticleVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

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

    @Test
    void shouldChainArticlesAcrossCategories() {
        // 构造：C1 (sort=1) → A1, A2;  C2 (sort=2) → A3;  C3 (sort=3) → A4
        LearnCategoryEntity c1 = insertCategory(null, "C1", 1);
        LearnCategoryEntity c2 = insertCategory(null, "C2", 2);
        LearnCategoryEntity c3 = insertCategory(null, "C3", 3);

        LearnArticleEntity a1 = insertArticle(c1.getId(), "A1", 1, LocalDateTime.now().minusDays(4), ArticleStatus.PUBLISHED);
        LearnArticleEntity a2 = insertArticle(c1.getId(), "A2", 2, LocalDateTime.now().minusDays(3), ArticleStatus.PUBLISHED);
        LearnArticleEntity a3 = insertArticle(c2.getId(), "A3", 1, LocalDateTime.now().minusDays(2), ArticleStatus.PUBLISHED);
        LearnArticleEntity a4 = insertArticle(c3.getId(), "A4", 1, LocalDateTime.now().minusDays(1), ArticleStatus.PUBLISHED);

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
        LearnArticleEntity a1 = insertArticle(c1.getId(), "A1", 1, LocalDateTime.now().minusDays(3), ArticleStatus.PUBLISHED);
        insertArticle(c1.getId(), "DRAFT", 2, LocalDateTime.now().minusDays(2), ArticleStatus.DRAFT);
        LearnArticleEntity a3 = insertArticle(c1.getId(), "A3", 3, LocalDateTime.now().minusDays(1), ArticleStatus.PUBLISHED);

        LearnArticleVO v1 = service.articleDetail(a1.getId());
        assertEquals(a3.getId(), v1.getNextArticle().getId());

        LearnArticleVO v3 = service.articleDetail(a3.getId());
        assertEquals(a1.getId(), v3.getPrevArticle().getId());
    }

    @Test
    void shouldUseUpdatedAtDescWhenSortEquals() {
        LearnCategoryEntity c1 = insertCategory(null, "C1", 1);
        // 两篇 sort 相同，updated_at 新的在前
        LearnArticleEntity older = insertArticle(c1.getId(), "Older", 1, LocalDateTime.now().minusDays(2), ArticleStatus.PUBLISHED);
        LearnArticleEntity newer = insertArticle(c1.getId(), "Newer", 1, LocalDateTime.now().minusDays(1), ArticleStatus.PUBLISHED);

        LearnArticleVO vNewer = service.articleDetail(newer.getId());
        assertNull(vNewer.getPrevArticle());
        assertEquals(older.getId(), vNewer.getNextArticle().getId());
    }

    @Test
    void shouldRespectCategoryDfsOrder() {
        // C1 (sort=1) 含子分类 C1.1 (sort=1)，C2 (sort=2)
        // DFS 前序：C1 → C1.1 → C2
        LearnCategoryEntity c1 = insertCategory(null, "C1", 1);
        LearnCategoryEntity c11 = insertCategory(c1.getId(), "C1.1", 1);
        LearnCategoryEntity c2 = insertCategory(null, "C2", 2);

        LearnArticleEntity aC1 = insertArticle(c1.getId(), "aC1", 1, LocalDateTime.now().minusDays(3), ArticleStatus.PUBLISHED);
        LearnArticleEntity aC11 = insertArticle(c11.getId(), "aC1.1", 1, LocalDateTime.now().minusDays(2), ArticleStatus.PUBLISHED);
        LearnArticleEntity aC2 = insertArticle(c2.getId(), "aC2", 1, LocalDateTime.now().minusDays(1), ArticleStatus.PUBLISHED);

        LearnArticleVO vC1 = service.articleDetail(aC1.getId());
        assertEquals(aC11.getId(), vC1.getNextArticle().getId());

        LearnArticleVO vC11 = service.articleDetail(aC11.getId());
        assertEquals(aC1.getId(), vC11.getPrevArticle().getId());
        assertEquals(aC2.getId(), vC11.getNextArticle().getId());
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

    private LearnArticleEntity insertArticle(Long categoryId, String title, int sort,
                                             LocalDateTime updatedAt, ArticleStatus status) {
        LearnArticleEntity e = new LearnArticleEntity();
        e.setCategoryId(categoryId);
        e.setTitle(title);
        e.setSummary("summary-" + title);
        e.setContentType(ContentType.MARKDOWN);
        e.setContent("# " + title);
        e.setStatus(status);
        e.setSort(sort);
        e.setPublishedAt(status == ArticleStatus.PUBLISHED ? updatedAt : null);
        articleMapper.insert(e);
        // updated_at 由 FieldFill.INSERT_UPDATE 自动写，这里显式 update 一次以控制排序字段
        e.setUpdatedAt(updatedAt);
        articleMapper.updateById(e);
        return e;
    }
}
```

- [ ] **Step 4: 运行测试，确认失败**

```bash
cd project/user/api
./mvnw -q test -Dtest=LearnBrowseServiceImplTest
```

预期：编译失败或断言失败——`LearnArticleVO` 上还没有 `prevArticle` / `nextArticle`，或 Service 还没有填充它们。

- [ ] **Step 5: 修改 `LearnBrowseServiceImpl.java`**

把 `articleDetail` 方法替换为下面版本，并新增三个私有方法。**保留**现有 `tree`、`buildTree`、`sortRecursive`、`categoryDetail`、`toVo` 方法不动。

```java
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
                .thenComparing(LearnArticleEntity::getSort, Comparator.nullsLast(Comparator.naturalOrder()))
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
```

同时在文件顶部补 import（若尚未有）：

```java
import com.aichuangzuo.user.modules.learn.vo.LearnArticleRefVO;
```

- [ ] **Step 6: 运行测试，确认通过**

```bash
cd project/user/api
./mvnw -q test -Dtest=LearnBrowseServiceImplTest
```

预期：BUILD SUCCESS，4 个测试全部通过。

- [ ] **Step 7: 手动 smoke 验证接口**

启动 user-api（默认 8081），用 curl 或浏览器访问：

```bash
curl -s http://localhost:8081/api/v1/user/learn/article/<某个已发布文章 id> | jq '.data | {id, title, prevArticle, nextArticle}'
```

预期：`prevArticle` 与 `nextArticle` 按阅读链顺序返回；首篇 `prevArticle` 为 `null`；末篇 `nextArticle` 为 `null`。

- [ ] **Step 8: 提交**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/vo/LearnArticleRefVO.java \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/vo/LearnArticleVO.java \
        project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/service/impl/LearnBrowseServiceImpl.java \
        project/user/api/src/test/java/com/aichuangzuo/user/modules/learn/service/LearnBrowseServiceImplTest.java
git commit -m "feat(user-api): 创作学院文章详情接口返回上一篇/下一篇

- LearnArticleVO 新增 prevArticle / nextArticle 引用字段
- Service 在 articleDetail 中构建跨分类阅读链（分类 DFS + sort ASC + updated_at DESC）
- 单测覆盖跨分类、跳过草稿、sort 相同用 updated_at、DFS 顺序四种场景

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 2: 前端 - 文章底部上下篇导航 + 切换滚顶部

**Files:**
- Modify: `project/user/web/src/components/learn/LearnContent.vue`
- Modify: `project/user/web/src/views/LearnIndex.vue`

**Interfaces:**
- Consumes:
  - 后端 `GET /learn/article/{id}` 返回的 `article.prevArticle` / `article.nextArticle`（`{ id, title, categoryName } | null`）。
  - 现有 `fetchCategoryTree()` 返回的分类树（含 `id` / `name` / `children`）。
- Produces:
  - `LearnContent.vue` 新增 prop `currentCategoryName: String`。
  - `LearnIndex.vue` 新增计算属性 `currentCategoryName`，通过 prop 传入 `LearnContent`。
  - 路由切换到 `/learn/article/:id` 后 `window.scrollTo({ top: 0, behavior: 'smooth' })`。

- [ ] **Step 1: 修改 `LearnContent.vue` —— 模板**

把 `<template>` 中 `<!-- 文章详情 -->` 那一段改为（在 `<article class="learn-content-body">` 与 `<footer class="learn-content-foot">` 之间插入 `<nav>`）：

```vue
    <!-- 文章详情 -->
    <template v-if="article">
      <header class="learn-content-head">
        <h1 class="learn-content-title">{{ article.title }}</h1>
        <p v-if="article.summary" class="learn-content-summary">{{ article.summary }}</p>
        <div class="learn-content-meta">
          发布于 {{ formatDate(article.publishedAt || article.updatedAt) }}
        </div>
      </header>
      <article class="learn-content-body">
        <LearnMarkdown v-if="article.contentType === 'markdown'" :source="article.content" />
        <LearnRichText v-else :html="article.content" />
      </article>

      <nav v-if="article.prevArticle || article.nextArticle" class="learn-nav">
        <router-link
          v-if="article.prevArticle"
          :to="`/learn/article/${article.prevArticle.id}`"
          class="learn-nav-card learn-nav-prev"
        >
          <span class="learn-nav-dir">← 上一篇</span>
          <span class="learn-nav-title">{{ article.prevArticle.title }}</span>
          <span
            v-if="currentCategoryName && article.prevArticle.categoryName !== currentCategoryName"
            class="learn-nav-cat"
          >《{{ article.prevArticle.categoryName }}》</span>
        </router-link>

        <router-link
          v-if="article.nextArticle"
          :to="`/learn/article/${article.nextArticle.id}`"
          class="learn-nav-card learn-nav-next"
        >
          <span class="learn-nav-dir">下一篇 →</span>
          <span class="learn-nav-title">{{ article.nextArticle.title }}</span>
          <span
            v-if="currentCategoryName && article.nextArticle.categoryName !== currentCategoryName"
            class="learn-nav-cat"
          >《{{ article.nextArticle.categoryName }}》</span>
        </router-link>
      </nav>

      <footer class="learn-content-foot">
        <router-link to="/login" class="learn-cta">想把自己的账号也做成这样？立即开始创作 →</router-link>
      </footer>
    </template>
```

- [ ] **Step 2: 修改 `LearnContent.vue` —— props**

把现有 `defineProps` 改为：

```js
defineProps({
  article: { type: Object, default: null },
  category: { type: Object, default: null },
  currentCategoryName: { type: String, default: '' }
})
```

- [ ] **Step 3: 修改 `LearnContent.vue` —— 样式**

在 `<style scoped>` 末尾追加：

```css
.learn-nav {
  display: flex;
  gap: 12px;
  margin: 32px 0;
}
.learn-nav-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px 20px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fff;
  color: #1a1a1a;
  text-decoration: none;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s;
  min-width: 0;
}
.learn-nav-card:hover {
  border-color: #FF2442;
  color: #FF2442;
}
.learn-nav-prev { text-align: left; align-items: flex-start; }
.learn-nav-next { text-align: right; align-items: flex-end; }
.learn-nav-dir {
  font-size: 12px;
  color: #999;
  font-weight: 500;
}
.learn-nav-card:hover .learn-nav-dir { color: #FF2442; }
.learn-nav-title {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-width: 100%;
}
.learn-nav-cat {
  font-size: 12px;
  color: #999;
  font-weight: 400;
}
@media (max-width: 991px) {
  .learn-nav { flex-direction: column; }
  .learn-nav-prev,
  .learn-nav-next { text-align: left; align-items: flex-start; }
}
```

- [ ] **Step 4: 修改 `LearnIndex.vue` —— 传入 `currentCategoryName`**

在 `<template>` 中 `<LearnContent>` 那一行加 prop：

```vue
        <LearnContent
          :article="currentArticle"
          :category="currentCategory"
          :current-category-name="currentCategoryName"
          @load-article="loadArticle"
        />
```

在 `<script setup>` 中，在 `activeCategoryId` 这个 `computed` 之后追加：

```js
// 反查当前文章所属分类的名称，用于跨分类跳转提示
const currentCategoryName = computed(() => {
  if (!currentArticle.value?.categoryId) return ''
  const targetId = currentArticle.value.categoryId
  const walk = nodes => {
    for (const n of nodes) {
      if (n.id === targetId) return n.name
      if (n.children?.length) {
        const found = walk(n.children)
        if (found) return found
      }
    }
    return ''
  }
  return walk(categoryTree.value)
})
```

- [ ] **Step 5: 修改 `LearnIndex.vue` —— 切文章滚顶部**

把现有 `watch(() => route.fullPath, bootstrap)` 改为：

```js
watch(() => route.fullPath, (newPath, oldPath) => {
  bootstrap()
  // 仅当切换的是文章（params.id 存在且变化）时滚动到顶部，分类切换不动
  const newId = route.params.id
  if (newId && newPath !== oldPath) {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
})
```

> **注**：`bootstrap` 内部会读 `route.params.id`，所以这里直接读即可。首次 `onMounted(bootstrap)` 不滚动（首屏已在顶部）。

- [ ] **Step 6: 手动验证**

启动 user-web dev（默认 22345）与 user-api（默认 8081），浏览器访问：

1. `http://localhost:22345/learn` → 选一个分类 → 进入第一篇文章
   - 预期：底部只看到「下一篇」卡片，没有「上一篇」。
2. 点「下一篇」→ 进入第二篇
   - 预期：两个卡片都在；页面自动滚到顶部；URL 变为新文章 id。
3. 一路点到末篇
   - 预期：只看到「上一篇」。
4. 找到跨分类的那一篇（`categoryName` 与当前文章不同）
   - 预期：按钮内标题下方出现 `《分类名》` 小字。
5. 窗口缩到 `< 992px`
   - 预期：两张卡片上下堆叠。

- [ ] **Step 7: 提交**

```bash
git add project/user/web/src/components/learn/LearnContent.vue \
        project/user/web/src/views/LearnIndex.vue
git commit -m "feat(user-web): 创作学院文章底部上一篇/下一篇导流

- LearnContent 正文与 CTA 之间插入两张导航卡片，首/末篇对应方向不渲染
- 跨分类时按钮内显示《分类名》提示，同分类隐藏
- LearnIndex 反查当前分类名传入；切文章后平滑滚动到顶部

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 3: e2e - 创作学院上下篇验证脚本

**Files:**
- Create: `tests/e2e/learn_article_nav.py`

**Interfaces:**
- Consumes:
  - 运行中的 user-web（`http://localhost:22345`）。
  - 运行中的 user-api + 已种子数据：至少 3 个分类、跨分类的 5 篇已发布文章。
  - 现有约定：`tests/e2e/learn_browsing.py` 的目录与截图输出风格。

- [ ] **Step 1: 编写脚本**

新建 `tests/e2e/learn_article_nav.py`：

```python
#!/usr/bin/env python3
"""用户端 - 创作学院文章底部「上一篇/下一篇」导航端到端验证。

前置条件：
- MySQL 启动且 Flyway 已迁移
- user-api 启动（默认 8081）
- user-web dev 启动（默认 http://localhost:22345）
- 已通过管理端录入至少 2 个分类、跨分类的 3 篇已发布文章（推荐命名为 P1 / P2 / P3，按 sort 顺序排列）
"""

import os
import sys
import time
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

USER_URL = os.environ.get("USER_URL", "http://localhost:22345")
SCREENSHOTS_DIR = Path(__file__).parent / "screenshots" / "learn_nav"
SCREENSHOTS_DIR.mkdir(parents=True, exist_ok=True)


def goto_article(page, article_id):
    page.goto(f"{USER_URL}/learn/article/{article_id}")
    time.sleep(0.8)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()

        # ---------- Desktop ----------
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()

        # 先去学院首页拿文章顺序，避免硬编码 id
        page.goto(f"{USER_URL}/learn")
        time.sleep(1.0)
        # 直接调后端 API 拿全学院第一/中间/最后一篇的 id，最稳
        # 这里用 page.evaluate 调 fetch，避免再引 requests 依赖
        first_id = page.evaluate("""
            async () => {
                const treeRes = await fetch('/api/v1/user/learn/category/tree');
                const tree = (await treeRes.json()).data || [];
                const orderedCatIds = [];
                const walk = nodes => nodes.forEach(n => {
                    orderedCatIds.push(n.id);
                    if (n.children && n.children.length) walk(n.children);
                });
                walk(tree);
                // 按分类顺序拉每分类文章，拼出全学院序列
                const seq = [];
                for (const cid of orderedCatIds) {
                    const r = await fetch(`/api/v1/user/learn/category/${cid}?page=1&size=100`);
                    const d = (await r.json()).data;
                    if (d && d.articles) d.articles.forEach(a => seq.push(a.id));
                }
                return seq;
            }
        """)
        if len(first_id) < 3:
            print(f"FAIL: need at least 3 published articles, got {len(first_id)}", file=sys.stderr)
            sys.exit(1)
        first, mid, last = first_id[0], first_id[len(first_id) // 2], first_id[-1]

        # 1. 首篇：只看到「下一篇」
        goto_article(page, first)
        expect(page.locator('.learn-nav-next')).to_be_visible()
        expect(page.locator('.learn-nav-prev')).to_have_count(0)
        page.screenshot(path=SCREENSHOTS_DIR / "01-first-only-next.png", full_page=True)

        # 2. 中间篇：两个按钮都在；点击「下一篇」跳转并滚顶部
        goto_article(page, mid)
        expect(page.locator('.learn-nav-prev')).to_be_visible()
        expect(page.locator('.learn-nav-next')).to_be_visible()
        page.screenshot(path=SCREENSHOTS_DIR / "02-mid-both.png", full_page=True)

        # 滚到底部再点下一篇
        page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        time.sleep(0.3)
        page.locator('.learn-nav-next').click()
        time.sleep(1.0)
        scroll_y = page.evaluate("window.scrollY")
        assert scroll_y < 50, f"after click next, scrollY should be near 0, got {scroll_y}"
        page.screenshot(path=SCREENSHOTS_DIR / "03-after-click-scroll-top.png", full_page=True)

        # 3. 末篇：只看到「上一篇」
        goto_article(page, last)
        expect(page.locator('.learn-nav-prev')).to_be_visible()
        expect(page.locator('.learn-nav-next')).to_have_count(0)
        page.screenshot(path=SCREENSHOTS_DIR / "04-last-only-prev.png", full_page=True)

        # 4. 跨分类提示：找一篇 next/prev 的 categoryName 与当前不同的，按钮内应有 《...》
        # 简化：遍历序列，访问每一篇，看是否能找到 .learn-nav-cat 至少出现一次
        found_cat_hint = False
        for aid in first_id:
            goto_article(page, aid)
            if page.locator('.learn-nav-cat').count() > 0:
                found_cat_hint = True
                page.screenshot(path=SCREENSHOTS_DIR / "05-cross-category-hint.png", full_page=True)
                break
        if not found_cat_hint:
            print("WARN: no cross-category hint found (may be expected if all articles are in one category)")

        ctx.close()

        # ---------- Mobile：上下堆叠 ----------
        ctx2 = browser.new_context(viewport={"width": 390, "height": 800})
        page2 = ctx2.new_page()
        goto_article(page2, mid)
        time.sleep(0.5)
        page2.screenshot(path=SCREENSHOTS_DIR / "06-mobile-stacked.png", full_page=True)
        # 验证堆叠：prev 的 bottom 应 <= next 的 top
        prev_box = page2.locator('.learn-nav-prev').bounding_box()
        next_box = page2.locator('.learn-nav-next').bounding_box()
        if prev_box and next_box:
            assert prev_box["y"] + prev_box["height"] <= next_box["y"] + 5, \
                f"cards should stack on mobile, prev bottom={prev_box['y']+prev_box['height']}, next top={next_box['y']}"

        ctx2.close()
        browser.close()
        print(f"OK screenshots -> {SCREENSHOTS_DIR}")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"FAIL: {e}", file=sys.stderr)
        sys.exit(1)
```

- [ ] **Step 2: 运行脚本**

确保 user-api 与 user-web 都已启动，且管理端至少录入跨 2 个分类的 3 篇已发布文章。

```bash
python3 tests/e2e/learn_article_nav.py
```

预期：输出 `OK screenshots -> tests/e2e/screenshots/learn_nav`，目录下至少生成 5 张截图。

- [ ] **Step 3: 提交**

```bash
git add tests/e2e/learn_article_nav.py
git commit -m "test(e2e): 创作学院上下篇导航验证脚本

- 覆盖首/中/末篇按钮显示、点击切换后滚顶部、跨分类提示、移动端堆叠
- 通过 fetch 学院 API 动态获取全学院文章序列，避免硬编码 id

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 自检结果

- **Spec 覆盖**：阅读链算法（Task 1 Step 5）、VO 扩展（Task 1 Step 1-2）、首/末篇边界（Task 1 Step 3 测试 + Task 2 Step 1 v-if）、跨分类提示（Task 2 Step 1 `learn-nav-cat`）、移动端堆叠（Task 2 Step 3 media query）、切换滚顶部（Task 2 Step 5）、单测四种场景（Task 1 Step 3）、e2e（Task 3）、提交拆分（每个 Task 末尾的 commit step）。所有 spec 要点均有对应 task。
- **占位符扫描**：无 TBD/TODO，所有代码块完整可直接执行。
- **类型一致性**：`LearnArticleRefVO` 字段名（`id/title/categoryName`）在 VO 定义、Service 填充、前端模板、e2e 选择器中一致；`prevArticle` / `nextArticle` 命名贯穿后端 VO、前端模板、e2e 测试。
