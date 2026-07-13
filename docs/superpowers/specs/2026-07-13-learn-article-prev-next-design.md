# 创作学院文章「上一篇 / 下一篇」导流设计

**日期**: 2026-07-13
**状态**: 已确认，待实现
**关联文件**:
- 后端：`project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/vo/LearnArticleVO.java`、`project/user/api/src/main/java/com/aichuangzuo/user/modules/learn/service/impl/LearnBrowseServiceImpl.java`
- 前端：`project/user/web/src/components/learn/LearnContent.vue`、`project/user/web/src/views/LearnIndex.vue`
- 测试：`project/user/api/src/test/java/.../learn/LearnBrowseServiceImplTest.java`（新增/扩展）、`tests/e2e/learn_article_nav.py`（新增）

---

## 1. 功能概述

在创作学院**文章详情页底部**增加「上一篇 / 下一篇」导航卡片，把整所学院拉通成**一条跨分类的阅读链**，让读者可以顺着读完所有内容。

- **范围**：全学院串联，跨分类自动衔接（不局限于当前文章所属分类）。
- **方向**：双向，同时提供「上一篇」和「下一篇」。
- **位置**：正文之后、现有 CTA（"想把自己的账号也做成这样？立即开始创作 →"）之前。

不改动现有分类列表页和分类树接口。

---

## 2. 设计决策

- **阅读链顺序**：
  1. 取分类树，按 DFS 前序展开（父 → 子，同级按 `sort ASC`），得到分类列表 `[C1, C2, ..., Cm]`。
  2. 每个分类下的已发布文章按 `sort ASC, updated_at DESC` 排序（与现有 `categoryDetail` 完全一致）。
  3. 按分类顺序把文章首尾相接，得到全局序列 `[A1, A2, ..., An]`。
- **计算位置**：后端 `LearnBrowseServiceImpl.articleDetail()` 内同步计算。一次性把所有已发布文章查出来（量级几十篇的小表），内存里构建序列后取邻居，不分页、不缓存。
- **跨分类提示**：当 `prev/next` 与当前文章不在同一分类时，按钮内显示一行小字 `《分类名》`；同分类时不显示，避免噪音。
- **首篇 / 末篇**：对应方向的按钮整个不渲染（不置灰），剩余按钮独占一行。
- **切换文章滚动**：顺手修一个 UX bug——`LearnIndex.vue` 路由切换后没有 `scrollTo`，读者从底部点击「下一篇」会停在新文章底部。改为 `window.scrollTo({ top: 0, behavior: 'smooth' })`。

---

## 3. 后端变更

### 3.1 VO 新增引用类型

在 `project/user/api/.../learn/vo/` 下新增 `LearnArticleRefVO.java`：

```java
package com.aichuangzuo.user.modules.learn.vo;

import lombok.Data;

@Data
public class LearnArticleRefVO {
    private Long id;
    private String title;
    private String categoryName;
}
```

### 3.2 `LearnArticleVO` 扩展

新增两个字段，首篇 / 末篇时为 `null`：

```java
private LearnArticleRefVO prevArticle;
private LearnArticleRefVO nextArticle;
```

### 3.3 Service 计算逻辑

在 `LearnBrowseServiceImpl.articleDetail(Long id)` 内：

```java
@Override
public LearnArticleVO articleDetail(Long id) {
    LearnArticleEntity current = articleMapper.selectOne(new QueryWrapper<LearnArticleEntity>()
            .eq("id", id)
            .eq("status", ArticleStatus.PUBLISHED.getCode()));
    if (current == null) return null;

    LearnArticleVO vo = toVo(current);

    // 构建全学院阅读链
    List<LearnArticleEntity> chain = buildReadingChain();
    int idx = -1;
    for (int i = 0; i < chain.size(); i++) {
        if (chain.get(i).getId().equals(id)) { idx = i; break; }
    }
    if (idx > 0)               vo.setPrevArticle(toRef(chain.get(idx - 1)));
    if (idx >= 0 && idx < chain.size() - 1) vo.setNextArticle(toRef(chain.get(idx + 1)));
    return vo;
}

private List<LearnArticleEntity> buildReadingChain() {
    // 1. 分类按 DFS 前序展开（sort ASC）
    List<LearnCategoryEntity> allCats = categoryMapper.selectList(null);
    List<Long> orderedCatIds = flattenCategoryIds(allCats);

    // 2. 一次性查所有已发布文章
    List<LearnArticleEntity> allPublished = articleMapper.selectList(new QueryWrapper<LearnArticleEntity>()
            .eq("status", ArticleStatus.PUBLISHED.getCode()));

    // 3. 按 (分类顺序, sort ASC, updated_at DESC) 内存排序
    Map<Long, Integer> catOrder = new HashMap<>();
    for (int i = 0; i < orderedCatIds.size(); i++) catOrder.put(orderedCatIds.get(i), i);

    allPublished.sort(Comparator
            .comparingInt((LearnArticleEntity a) -> catOrder.getOrDefault(a.getCategoryId(), Integer.MAX_VALUE))
            .thenComparing(LearnArticleEntity::getSort)
            .thenComparing(LearnArticleEntity::getUpdatedAt, Comparator.reverseOrder()));
    return allPublished;
}

private List<Long> flattenCategoryIds(List<LearnCategoryEntity> cats) {
    // 复用 buildTree 的顺序逻辑，输出 DFS 前序的 id 列表
    // 实现时抽一个共用方法，避免和 buildTree 重复维护排序规则
}

private LearnArticleRefVO toRef(LearnArticleEntity e) {
    LearnArticleRefVO r = new LearnArticleRefVO();
    r.setId(e.getId());
    r.setTitle(e.getTitle());
    LearnCategoryEntity c = categoryMapper.selectById(e.getCategoryId());
    r.setCategoryName(c == null ? null : c.getName());
    return r;
}
```

> **注**：`toRef` 里每篇文章都 `selectById` 一次分类名，会产生 N 次额外查询。实现时改为在 `buildReadingChain` 里一次性 `selectBatchIds` 把分类装进 `Map<Long, String>` 复用。

### 3.4 接口契约

`GET /api/v1/user/learn/article/{id}` 响应示例：

```jsonc
{
  "code": 0,
  "data": {
    "id": 12,
    "categoryId": 3,
    "title": "如何写出第一条爆款标题",
    "summary": "...",
    "contentType": "markdown",
    "content": "...",
    "publishedAt": "2026-07-01T10:00:00",
    "updatedAt": "2026-07-01T10:00:00",
    "prevArticle": { "id": 11, "title": "...", "categoryName": "新手入门" },
    "nextArticle": { "id": 13, "title": "...", "categoryName": "进阶技巧" }
  },
  "message": "success"
}
```

`/learn/category/tree` 和 `/learn/category/{id}` **不改动**。

---

## 4. 前端变更

### 4.1 `LearnContent.vue` 新增上下篇区块

模板插在 `learn-content-body` 之后、`learn-content-foot` 之前：

```vue
<nav v-if="article && (article.prevArticle || article.nextArticle)" class="learn-nav">
  <router-link
    v-if="article.prevArticle"
    :to="`/learn/article/${article.prevArticle.id}`"
    class="learn-nav-card learn-nav-prev"
  >
    <span class="learn-nav-dir">← 上一篇</span>
    <span class="learn-nav-title">{{ article.prevArticle.title }}</span>
    <span
      v-if="article.prevArticle.categoryName !== currentCategoryName"
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
      v-if="article.nextArticle.categoryName !== currentCategoryName"
      class="learn-nav-cat"
    >《{{ article.nextArticle.categoryName }}》</span>
  </router-link>
</nav>
```

`currentCategoryName` 由父组件 `LearnIndex.vue` 在加载文章时一并传入（通过 `fetchCategoryTree` 结果反查当前 `article.categoryId` 对应的分类名，避免多一次后端往返）。

### 4.2 样式

- 容器 `learn-nav`：`display: flex; gap: 12px; margin: 32px 0;`。
- 卡片 `learn-nav-card`：`flex: 1; padding: 16px 20px; border: 1px solid #eee; border-radius: 8px; background: #fff; cursor: pointer;` 标题最多 2 行省略。
- hover：`border-color: #FF2442; color: #FF2442;`（品牌色与现有 hover 一致）。
- 上一篇 `text-align: left;`，下一篇 `text-align: right;`。
- 移动端（`< 992px`）：`flex-direction: column;`，两张卡片上下堆叠，文字一律左对齐。

### 4.3 切换文章滚动到顶部

`LearnIndex.vue` 的 `bootstrap()` 末尾（`onMounted` 与 `watch` 共用）加：

```js
window.scrollTo({ top: 0, behavior: 'smooth' })
```

仅在 `route.params.id` 变化时执行（避免分类切换也滚动）。

---

## 5. 边界与异常

| 场景 | 行为 |
|---|---|
| 全学院只有 1 篇已发布文章 | `prevArticle` / `nextArticle` 均为 `null`，整个 `learn-nav` 不渲染 |
| 当前文章是首篇 | 左侧按钮不渲染，右侧独占一行，靠右对齐 |
| 当前文章是末篇 | 右侧按钮不渲染，左侧独占一行，靠左对齐 |
| 某分类下全是草稿 / 已下线 | 该分类在序列中被跳过（只查 `status = PUBLISHED`），不影响其他分类串联 |
| `sort` 相同 | `updated_at DESC` 兜底（与 `categoryDetail` 排序一致） |
| 文章被管理端下线，读者停在旧页面 | 点击上下篇后若 404，走现有 `NotFoundException` 流程 |
| 跨分类衔接 | 按钮内标题下方显示 `《分类名》`；同分类不显示 |

---

## 6. 测试

### 6.1 后端单元测试

`LearnBrowseServiceImplTest` 新增用例：

- 构造 3 个分类（含父子结构），5 篇已发布文章跨分类分布。
- 断言：每篇文章的 `prevArticle` / `nextArticle` 与期望序列一致；首篇 `prevArticle == null`；末篇 `nextArticle == null`。
- 断言：含 `DRAFT` 状态文章时，该文章被跳过，相邻已发布文章直接相连。
- 断言：同 `sort` 时按 `updated_at DESC` 决定先后。

### 6.2 前端 e2e（Playwright）

新增 `tests/e2e/learn_article_nav.py`，沿用现有 e2e 脚本模式：

1. 访问全学院首篇 → 仅「下一篇」可见。
2. 访问中间篇 → 两个按钮都在；点击「下一篇」跳转正确，且页面滚动到顶部。
3. 访问跨分类的那一篇 → 按钮内显示 `《分类名》` 提示。
4. 访问末篇 → 仅「上一篇」可见。
5. 视口缩到 `800px` → 两个按钮上下堆叠。

### 6.3 手测清单

- 首页 → 创作学院 → 任一分类 → 任一篇文章 → 底部出现上下篇卡片。
- 点击「下一篇」连续走完整个学院，无死链、无空白分类。
- 管理端把某篇文章下线后，刷新前端：该文章从序列中消失，前后两篇直接相连。

---

## 7. 提交拆分

| Commit | 范围 |
|---|---|
| `feat(user-api): 创作学院文章详情接口返回上一篇/下一篇` | `LearnArticleRefVO` 新增、`LearnArticleVO` 扩展、`LearnBrowseServiceImpl` 计算逻辑、单元测试 |
| `feat(user-web): 创作学院文章底部上一篇/下一篇导流` | `LearnContent.vue` 上下篇区块与样式、`LearnIndex.vue` 滚动修复、`tests/e2e/learn_article_nav.py` |
