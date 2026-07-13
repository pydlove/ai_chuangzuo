# 用户端创作-生成队列改查数据库 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `/console/create` 右侧"生成队列"面板及所有消费 `aichuangzuo_generation_queue` 的位置改为按当前登录用户从数据库查询，并清理本地缓存死代码。

**Architecture:** 前端 CreateIndex 面板复用已有的 `GET /api/v1/user/generation-tasks`；ConsoleLayout/MineIndex 的本月数改调新增的 `GET /api/v1/user/articles/monthly-count`；其余 localStorage 消费方全部删除。

**Tech Stack:** Vue 3 + Ant Design Vue（前端），Spring Boot + MyBatis-Plus（后端），Playwright（E2E）。

## Global Constraints

- 后端 JDK 17，Spring Boot，MyBatis-Plus。
- 数据库表名 `u_article`，逻辑删除字段 `is_deleted`（`@TableLogic`）。
- 用户 ID 从 `SecurityUserContext.getCurrentUserId()` 获取。
- 前端请求统一走 `@/api/auth.js` 的 `api` 实例（已带 JWT）。
- 不用的代码必须删掉，不留注释或保留。

---

### Task 1: 后端 monthly-count 接口 + 单元测试

**Files:**
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/article/service/ArticleService.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/article/service/impl/ArticleServiceImpl.java`
- Modify: `project/user/api/src/main/java/com/aichuangzuo/user/modules/article/controller/ArticleController.java`
- Test: `project/user/api/src/test/java/com/aichuangzuo/user/modules/article/service/ArticleServiceMonthlyCountTest.java`

**Interfaces:**
- Consumes: 无
- Produces:
  - `ArticleService.monthlyCount(Long userId): Long`
  - `GET /api/v1/user/articles/monthly-count` → `Result<Long>`

- [ ] **Step 1: 写失败测试**

创建 `project/user/api/src/test/java/com/aichuangzuo/user/modules/article/service/ArticleServiceMonthlyCountTest.java`：

```java
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

import java.time.LocalDateTime;

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
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd project/user/api && mvn test -Dtest=ArticleServiceMonthlyCountTest
```

Expected: 编译失败，`monthlyCount` 方法不存在。

- [ ] **Step 3: 实现 Service**

修改 `ArticleService.java`，新增：

```java
    /**
     * 查询用户本月已生成作品数。
     */
    Long monthlyCount(Long userId);
```

修改 `ArticleServiceImpl.java`，新增：

```java
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
```

- [ ] **Step 4: 实现 Controller**

修改 `ArticleController.java`，在 `delete` 方法后新增：

```java
    /**
     * 查询当前用户本月已生成作品数。
     */
    @Operation(summary = "本月已生成作品数")
    @GetMapping("/monthly-count")
    public Result<Long> monthlyCount() {
        Long userId = SecurityUserContext.getCurrentUserId();
        return Result.success(articleService.monthlyCount(userId));
    }
```

- [ ] **Step 5: 运行测试确认通过**

```bash
cd project/user/api && mvn test -Dtest=ArticleServiceMonthlyCountTest
```

Expected: PASS。

- [ ] **Step 6: 提交**

```bash
git add project/user/api/src/main/java/com/aichuangzuo/user/modules/article/service/ArticleService.java \
       project/user/api/src/main/java/com/aichuangzuo/user/modules/article/service/impl/ArticleServiceImpl.java \
       project/user/api/src/main/java/com/aichuangzuo/user/modules/article/controller/ArticleController.java \
       project/user/api/src/test/java/com/aichuangzuo/user/modules/article/service/ArticleServiceMonthlyCountTest.java
git commit -m "feat(user-api): 作品本月生成数统计接口"
```

---

### Task 2: CreateIndex 右侧面板改查后端

**Files:**
- Modify: `project/user/web/src/views/console/CreateIndex.vue`

**Interfaces:**
- Consumes:
  - `listGenerationTasks({ page: 1, pageSize: 5 })` from `@/api/generation.js`
  - `GenerationTaskVO` 字段：`id`, `status`, `title`, `wordLimitTarget`, `createdAt`, `progressPct`
- Produces: 无

- [ ] **Step 1: 替换 loadMiniQueue 实现**

把 `CreateIndex.vue` 中 `loadMiniQueue` 函数改为：

```js
const loadMiniQueue = async () => {
  try {
    const data = await listGenerationTasks({ page: 1, pageSize: 5 })
    miniQueueList.value = (data.list || []).map(t => ({
      id: t.id,
      title: t.title || t.inputParam?.title || '未命名',
      platform: t.inputParam?.platform || '未选择',
      wordCount: t.wordLimitTarget || 0,
      status: mapStatus(t.status),
      progress: t.progressPct || 0,
      createdAt: t.createdAt,
      completedAt: t.completedAt
    }))
  } catch (e) {
    miniQueueList.value = []
  }
}

const mapStatus = (code) => {
  return code === 0 ? 'queued' : code === 1 ? 'generating' : code === 2 ? 'completed' : code === 3 ? 'failed' : 'queued'
}
```

- [ ] **Step 2: 修改轮询间隔**

把 `onMounted` 中的 `setInterval(loadMiniQueue, 2000)` 改为：

```js
setInterval(loadMiniQueue, 5000)
```

- [ ] **Step 3: 修改"查看更多"链接**

把模板中 `router.push('/console/works')` 改为 `router.push('/console/ai-generate')`。

- [ ] **Step 4: 删除 mock 死代码**

删除 `CreateIndex.vue` 中以下代码块：

- `generateMockContent` 函数（约 1497-1505 行）
- `addToMiniQueue` 函数（约 1507-1544 行）
- `continueGeneration` 函数（约 786-800 行）
- `persistCompletedArticle` 函数（约 1550-1565 行）
- `saveMiniQueue` 函数（约 1546-1548 行）
- `WORKS_KEY` 常量（如存在）

同时删除 `saveArticle` 的 import（如仅被 `persistCompletedArticle` 使用）。

- [ ] **Step 5: 手动验证**

1. 启动前端：`cd project/user/web && npm run dev`
2. 登录后打开 `/console/create`
3. 确认右侧面板显示真实任务（空或已有数据）
4. 在 `/console/ai-generate` 提交一个任务，回到 `/console/create`，5s 内面板出现新任务

- [ ] **Step 6: 提交**

```bash
git add project/user/web/src/views/console/CreateIndex.vue
git commit -m "feat(user-web): 创作页生成队列面板改查后端"
```

---

### Task 3: ConsoleLayout 与 MineIndex 本月数/hasWorks 改查后端

**Files:**
- Modify: `project/user/web/src/api/article.js`
- Modify: `project/user/web/src/views/console/ConsoleLayout.vue`
- Modify: `project/user/web/src/views/console/MineIndex.vue`

**Interfaces:**
- Consumes:
  - `getMonthlyCount(): Promise<number>` from `@/api/article.js`
  - `listArticles({ page: 1, pageSize: 1 }): Promise<{ total: number }>` from `@/api/article.js`
- Produces: 无

- [ ] **Step 1: api/article.js 新增 getMonthlyCount**

在 `project/user/web/src/api/article.js` 末尾新增：

```js
/**
 * 查询当前用户本月已生成作品数。
 * @returns {Promise<number>}
 */
export function getMonthlyCount() {
  return api.get('/articles/monthly-count').then((res) => res.data)
}
```

- [ ] **Step 2: ConsoleLayout.vue 改造**

1. 在 import 区增加：
   ```js
   import { listArticles, getMonthlyCount } from '@/api/article.js'
   ```
2. 删除 `WORKS_KEY` 常量。
3. 把 `hasWorks` 改为异步函数：
   ```js
   const hasWorks = async () => {
     try {
       const data = await listArticles({ page: 1, pageSize: 1 })
       return (data.total || 0) > 0
     } catch {
       return false
     }
   }
   ```
4. 把 `readMonthlyWorks` 改为异步函数：
   ```js
   const readMonthlyWorks = async () => {
     try {
       return await getMonthlyCount()
     } catch {
       return 0
     }
   }
   ```
5. 把 `monthlyWorks` 初始化改为：
   ```js
   const monthlyWorks = ref(0)
   ```
6. 把 `guideBannerVisible` 初始化改为：
   ```js
   const guideBannerVisible = ref(!localStorage.getItem(GUIDE_BANNER_DISMISSED_KEY))
   ```
7. 在 `onMounted` 中增加：
   ```js
   monthlyWorks.value = await readMonthlyWorks()
   guideBannerVisible.value = guideBannerVisible.value && !(await hasWorks())
   ```

- [ ] **Step 3: MineIndex.vue 改造**

1. 在 import 区增加：
   ```js
   import { getMonthlyCount } from '@/api/article.js'
   ```
2. 删除 `WORKS_KEY` 常量。
3. 把 `monthlyWorks` computed 改为 ref + onMounted：
   ```js
   const monthlyWorks = ref(0)
   onMounted(async () => {
     try {
       monthlyWorks.value = await getMonthlyCount()
     } catch {
       monthlyWorks.value = 0
     }
   })
   ```

- [ ] **Step 4: 手动验证**

1. 打开 `/console/mine`，确认"本月已生成"显示正确数字。
2. 打开任意 console 页面，确认玩法指南横幅逻辑正常（有作品时不显示）。

- [ ] **Step 5: 提交**

```bash
git add project/user/web/src/api/article.js \
       project/user/web/src/views/console/ConsoleLayout.vue \
       project/user/web/src/views/console/MineIndex.vue
git commit -m "feat(user-web): 本月已生成/玩法指南横幅改查后端"
```

---

### Task 4: 清理 articleStorage、PreviewIndex、EditIndex、QueueIndex

**Files:**
- Modify: `project/user/web/src/utils/articleStorage.js`
- Modify: `project/user/web/src/views/console/PreviewIndex.vue`
- Modify: `project/user/web/src/views/console/EditIndex.vue`
- Delete: `project/user/web/src/views/console/QueueIndex.vue`

**Interfaces:**
- Consumes: 无
- Produces: 无

- [ ] **Step 1: 清理 articleStorage.js**

删除 `QUEUE_KEY` 常量和 `syncArticleToQueue` 函数，保留 `CURRENT_ARTICLE_KEY`、`loadCurrentArticle`、`saveCurrentArticle`。

- [ ] **Step 2: 清理 PreviewIndex.vue**

1. 删除 import 中的 `syncArticleToQueue`。
2. 删除 `syncArticleToQueue(updated)` 调用。

- [ ] **Step 3: 清理 EditIndex.vue**

1. 删除 import 中的 `syncArticleToQueue`。
2. 删除 `syncArticleToQueue(article)` 调用。

- [ ] **Step 4: 删除 QueueIndex.vue**

```bash
git rm project/user/web/src/views/console/QueueIndex.vue
```

- [ ] **Step 5: 全局检查无残留**

```bash
grep -r "syncArticleToQueue" project/user/web/src/
grep -r "aichuangzuo_generation_queue" project/user/web/src/
grep -r "QueueIndex" project/user/web/src/
```

Expected: 无输出（除 router 中 `/console/queue` 的重定向外）。

- [ ] **Step 6: 提交**

```bash
git add project/user/web/src/utils/articleStorage.js \
       project/user/web/src/views/console/PreviewIndex.vue \
       project/user/web/src/views/console/EditIndex.vue \
       project/user/web/src/views/console/QueueIndex.vue
git commit -m "refactor(user-web): 清理生成队列 localStorage 死代码"
```

---

### Task 5: E2E 测试清理

**Files:**
- Modify or Delete: `tests/e2e/verify_create_queue_overflow.py`
- Modify or Delete: `tests/e2e/verify_queue_limit.py`
- Check: `tests/e2e/works_search_full_audit.py`
- Check: `tests/e2e/verify_works_search_filter.py`
- Check: `tests/e2e/verify_richtext_toolbar.py`
- Check: `tests/e2e/verify_create_mobile_export.py`
- Check: `tests/e2e/verify_create_cards.py`
- Check: `tests/e2e/verify_console_content_editing.py`
- Check: `tests/e2e/verify_cards_themes.py`
- Check: `tests/e2e/inspect_works_btn.py`
- Check: `tests/e2e/dark_audit.py`

**Interfaces:**
- Consumes: 无
- Produces: 无

- [ ] **Step 1: 逐个检查并处理**

对每个文件执行：

```bash
grep -n "aichuangzuo_generation_queue" tests/e2e/<file>.py
```

- 如果脚本仅用于验证旧 mock 队列（如 `verify_create_queue_overflow.py`），直接删除。
- 如果脚本核心功能不依赖队列数据，但包含 localStorage 清理/播种，移除相关行。
- 如果脚本已不适用于当前路由/页面，直接删除。

- [ ] **Step 2: 全局确认**

```bash
grep -r "aichuangzuo_generation_queue" tests/e2e/
```

Expected: 无输出。

- [ ] **Step 3: 提交**

```bash
git add tests/e2e/
git commit -m "test(e2e): 清理引用生成队列 localStorage 的脚本"
```
