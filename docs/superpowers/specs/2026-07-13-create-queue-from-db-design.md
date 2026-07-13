# 用户端-创作-生成队列改查数据库（按用户）设计

## 背景

`/console/create`（`CreateIndex.vue`）右侧"生成队列"面板目前从 `localStorage.aichuangzuo_generation_queue` 读取 mock 数据。点击"生成文章"后实际跳转 `/console/ai-generate`（`GenerationQueueIndex.vue`），由后端真实队列处理；但 `CreateIndex.vue` 里仍保留一套本地 mock 生成逻辑（`addToMiniQueue` / `generateMockContent` / `continueGeneration` / `persistCompletedArticle` / `saveMiniQueue`），已成为死代码。

同时，`ConsoleLayout.vue`（玩法指南横幅、本月生成数）、`MineIndex.vue`（本月已生成）、`QueueIndex.vue`（孤儿页）、`articleStorage.js`（`syncArticleToQueue`）都消费同一个 localStorage key。本次一并清理。

## 目标

1. `CreateIndex.vue` 右侧"生成队列"面板数据改为按当前登录用户从数据库查询。
2. 清理所有消费 `aichuangzuo_generation_queue` 的本地缓存逻辑。
3. 新增后端统计接口支持"本月已生成 X 篇"。

## 范围

### 包含

- `CreateIndex.vue` 右侧面板改查后端
- 后端新增 `GET /api/v1/user/articles/monthly-count`
- `ConsoleLayout.vue` / `MineIndex.vue` 本月数改查后端
- `ConsoleLayout.vue` `hasWorks()` 改查后端
- 删除 `QueueIndex.vue`
- 删除 `articleStorage.js` 中 `syncArticleToQueue` 和 `QUEUE_KEY`
- 删除 `CreateIndex.vue` 中 mock 死代码
- 更新/删除引用 `aichuangzuo_generation_queue` 的 e2e 测试

### 不包含

- `GenerationQueueIndex.vue`（`/console/ai-generate`）的现有表单、提交、轮询逻辑
- 用户端作品列表 `/console/works` 的现有逻辑
- 会员额度/队列上限的现有校验逻辑

## 架构与数据流

```
用户打开 /console/create
  → CreateIndex.vue onMounted
  → GET /api/v1/user/generation-tasks?page=1&pageSize=5
  → 后端 SecurityUserContext.getCurrentUserId() 过滤
  → GenerationTaskPageVO { list, total, page, pageSize }
  → 前端按 status 映射 UI：0=排队中 1=生成中 2=已完成 3=失败
  → setInterval 5s 轮询

用户点击"生成文章"
  → 仍跳转 /console/ai-generate（现有行为不变）
  → GenerationQueueIndex.vue 提交任务并轮询单任务进度（现有逻辑不变）

ConsoleLayout.vue / MineIndex.vue 挂载
  → GET /api/v1/user/articles?page=1&pageSize=1 → total>0 即 hasWorks
  → GET /api/v1/user/articles/monthly-count → 本月数
```

## 前端改动

### `CreateIndex.vue`

- 右侧面板数据从 `listGenerationTasks({ page: 1, pageSize: 5 })` 获取。
- 轮询间隔 5s。
- 状态映射：
  - `0` → `queued` / 排队中 / 蓝色
  - `1` → `generating` / 生成中 / 处理中
  - `2` → `completed` / 已完成 / 绿色
  - `3` → `failed` / 失败 / 红色
- "查看更多 →" 从 `/console/works` 改为 `/console/ai-generate`。
- 删除以下死代码：
  - `addToMiniQueue`
  - `generateMockContent`
  - `continueGeneration`
  - `persistCompletedArticle`
  - `saveMiniQueue`
  - `WORKS_KEY`
  - 相关 `setInterval(loadMiniQueue, 2000)`

### `api/article.js`

新增：

```js
export function getMonthlyCount() {
  return api.get('/articles/monthly-count').then((res) => res.data)
}
```

### `ConsoleLayout.vue`

- `hasWorks()` 改为异步：调 `listArticles({ page: 1, pageSize: 1 })`，`total > 0` 返回 true。
- `readMonthlyWorks()` 改为异步：调 `getMonthlyCount()`。
- 删除 `WORKS_KEY`。
- `monthlyWorks` 保持 `ref(0)`，在 `onMounted` 里异步赋值。
- `guideBannerVisible` 初始化为 `!localStorage.getItem(GUIDE_BANNER_DISMISSED_KEY)`，在 `onMounted` 里再与 `!(await hasWorks())` 取与。

### `MineIndex.vue`

- `monthlyWorks` 从 computed 改为 ref + onMounted 调 `getMonthlyCount()`。
- 删除 `WORKS_KEY`。

### `QueueIndex.vue`

整文件删除。路由 `/console/queue` 已重定向到 `/console/create`，无引用。

### `utils/articleStorage.js`

- 删除 `QUEUE_KEY`。
- 删除 `syncArticleToQueue`。
- 保留 `CURRENT_ARTICLE_KEY`、`loadCurrentArticle`、`saveCurrentArticle`。

### `PreviewIndex.vue` / `EditIndex.vue`

- 删除 `syncArticleToQueue` 的 import 和调用（编辑保存后不再写 localStorage 队列）。

## 后端改动

### `ArticleController.java`

新增：

```java
@Operation(summary = "本月已生成作品数")
@GetMapping("/monthly-count")
public Result<Long> monthlyCount() {
    Long userId = SecurityUserContext.getCurrentUserId();
    return Result.success(articleService.monthlyCount(userId));
}
```

### `ArticleService.java` / `ArticleServiceImpl.java`

新增 `monthlyCount(Long userId)`：

- 计算当月第一天 00:00:00 和下个月第一天 00:00:00。
- 调 Mapper 查询 `completed_at` 在区间内、`deleted = 0`、`user_id = ?` 的记录数。

### `ArticleMapper.java`

新增方法（注解或 XML）：

```java
@Select("SELECT COUNT(*) FROM user_article WHERE user_id = #{userId} AND deleted = 0 AND completed_at >= #{start} AND completed_at < #{end}")
Long selectMonthlyCount(@Param("userId") Long userId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);
```

## 测试

### 后端单元测试

`ArticleServiceMonthlyCountTest`：

- Mock `ArticleMapper.selectMonthlyCount` 返回 3。
- 调用 `monthlyCount(userId)`。
- 断言返回 3，且 Mapper 参数 `start`/`end` 为当月第一天和下月第一天。

### 前端手动验证

1. 登录用户 A，提交 1 个生成任务。
2. 打开 `/console/create`，右侧面板显示该任务（状态=排队中/生成中）。
3. 任务完成后刷新，状态变为已完成。
4. 打开 `/console/ai-generate`，历史列表显示同一任务。
5. 打开 `/console/mine`，"本月已生成"显示正确数字。
6. 登录用户 B，确认看不到用户 A 的任务。

### E2E 测试

以下脚本引用 `aichuangzuo_generation_queue`，需更新或删除：

- `tests/e2e/verify_create_queue_overflow.py` — 改从 API mock 或删除
- `tests/e2e/verify_queue_limit.py` — 删除 localStorage 清理逻辑
- `tests/e2e/works_search_full_audit.py` — 检查是否仍需要
- `tests/e2e/verify_works_search_filter.py` — 检查是否仍需要
- `tests/e2e/verify_richtext_toolbar.py` — 检查是否仍需要
- `tests/e2e/verify_create_mobile_export.py` — 检查是否仍需要
- `tests/e2e/verify_create_cards.py` — 检查是否仍需要
- `tests/e2e/verify_console_content_editing.py` — 检查是否仍需要
- `tests/e2e/verify_cards_themes.py` — 检查是否仍需要
- `tests/e2e/inspect_works_btn.py` — 检查是否仍需要
- `tests/e2e/dark_audit.py` — 检查是否仍需要

## 风险与回滚

- **风险**：`monthly-count` 接口 SQL 性能。`user_article` 表数据量大时，`completed_at` 范围查询需走索引。确认 `user_id + deleted + completed_at` 有合适索引。
- **回滚**：前端回退到 localStorage 读取即可；后端接口可保留不影响旧逻辑。

## 后续

- 考虑将 `GenerationQueueIndex.vue` 和 `CreateIndex.vue` 的查询逻辑收敛为共享 composable（当前不做，保持简单）。
- 考虑把"本月已生成"并入用户额度接口，减少一次请求（当前不做，保持接口单一职责）。
