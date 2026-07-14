# 创作学院 UI 中度打磨 实施方案

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 对创作学院用户端做 6 处定向 UI 打磨：Hero 区、侧边栏图标与层级、面包屑 + 元信息条、上下篇卡片 + CTA、空状态、分类列表卡片化。

**Architecture:** 纯前端改动，不碰后端。复用已安装的 `@ant-design/icons-vue` 做图标，样式严格遵循 `docs/design/design-system.md` 的 `#FF2442` 品牌色体系。改动集中在 3 个 Vue 文件 + 1 个新建 JS 映射文件。

**Tech Stack:** Vue 3 + Vue Router 4 + Ant Design Vue + `@ant-design/icons-vue` + Playwright (e2e)。

## Global Constraints

- 不新增任何后端接口或字段。
- 不引新的 UI 库或 icon 库（只用已有的 `@ant-design/icons-vue` v7）。
- 品牌色严格用 `#FF2442`，浅红背景 `#FFF5F7`，hover 红 `#e61e3a`。
- 圆角：卡片 `12px`，小元素 `8px`，胶囊 `9999px`。
- 阅读时长估算：`Math.ceil(content.length / 300)`，不新增后端字段。
- Hero 副标题静态文案 `从 0 到 1 的自媒体实战指南`，不显示篇数。
- 移动端断点 `< 992px`，所有改动需正常堆叠。
- 不做暗色主题适配。

---

## 文件结构

| 操作 | 路径 | 责任 |
|---|---|---|
| 新建 | `project/user/web/src/components/learn/learnCategoryIcons.js` | 顶级分类 → Ant Design 图标映射 |
| 修改 | `project/user/web/src/views/LearnIndex.vue` | Hero 区、`currentCategoryPath` / `topCategories` computed、样式 |
| 修改 | `project/user/web/src/components/learn/LearnSidebarNode.vue` | 图标渲染、`›/∨` 折叠图标、层级指示线、选中圆点 |
| 修改 | `project/user/web/src/components/learn/LearnContent.vue` | 面包屑、元信息条、上下篇卡片升级、CTA 卡片、空状态、分类列表卡片 |
| 新建 | `tests/e2e/learn_ui_polish.py` | e2e 验证脚本 |

---

### Task 1: Hero 区 + 侧边栏升级

**Files:**
- Create: `project/user/web/src/components/learn/learnCategoryIcons.js`
- Modify: `project/user/web/src/components/learn/LearnSidebarNode.vue`
- Modify: `project/user/web/src/views/LearnIndex.vue`

**Interfaces:**
- Consumes: 现有 `categoryTree`（含 `id/name/children/sort`）、`activeId`。
- Produces:
  - `learnCategoryIcons.js` 导出 `CATEGORY_ICONS: Record<string, Component>`
  - `LearnSidebarNode.vue` 渲染顶级分类图标 + `›/∨` 折叠图标 + 选中圆点 + 层级竖线
  - `LearnIndex.vue` 渲染 Hero 区

- [ ] **Step 1: 新建 `learnCategoryIcons.js`**

```js
import {
  AimOutlined,
  RocketOutlined,
  ThunderboltOutlined,
  RiseOutlined,
  IdcardOutlined,
  WalletOutlined
} from '@ant-design/icons-vue'

/**
 * 创作学院顶级分类 → Ant Design 图标映射。
 * 找不到映射时不渲染图标（fallback 到 caret 占位）。
 */
export const CATEGORY_ICONS = {
  '内容定位': AimOutlined,
  '平台运营技巧': RocketOutlined,
  '爆款方法论': ThunderboltOutlined,
  '涨粉与流量增长': RiseOutlined,
  'IP 打造与人设': IdcardOutlined,
  '变现路径': WalletOutlined
}
```

- [ ] **Step 2: 修改 `LearnSidebarNode.vue` —— 模板**

把 `<template>` 改为：

```vue
<template>
  <li class="learn-tree-node">
    <div
      :class="['learn-tree-row', {
        active: node.id === activeId,
        expandable: hasChildren,
        'top-level': depth === 0,
        'child-level': depth > 0
      }]"
      :style="{ paddingLeft: `${depth * 16 + 12}px` }"
      @click="onClick"
    >
      <span v-if="hasChildren" class="learn-tree-caret">{{ open ? '∨' : '›' }}</span>
      <span v-else class="learn-tree-caret-spacer"></span>
      <component
        v-if="depth === 0 && iconComponent"
        :is="iconComponent"
        class="learn-tree-icon"
      />
      <span v-if="node.id === activeId" class="learn-tree-dot"></span>
      <span class="learn-tree-label">{{ node.name }}</span>
    </div>
    <ul v-if="open && hasChildren" class="learn-tree-children">
      <LearnSidebarNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :depth="depth + 1"
        :active-id="activeId"
        @select="(id) => $emit('select', id)"
      />
    </ul>
  </li>
</template>
```

- [ ] **Step 3: 修改 `LearnSidebarNode.vue` —— script**

把 `<script setup>` 改为：

```js
import { computed, ref } from 'vue'
import { CATEGORY_ICONS } from './learnCategoryIcons'

const props = defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 },
  activeId: { type: Number, default: null }
})
const emit = defineEmits(['select'])

const open = ref(props.depth < 1)
const hasChildren = computed(() => Array.isArray(props.node.children) && props.node.children.length > 0)
const iconComponent = computed(() => CATEGORY_ICONS[props.node.name] || null)

function onClick() {
  if (hasChildren.value) {
    open.value = !open.value
  }
  emit('select', props.node.id)
}
```

- [ ] **Step 4: 修改 `LearnSidebarNode.vue` —— 样式**

把 `<style scoped>` 改为：

```css
.learn-tree-row {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 12px; cursor: pointer; user-select: none;
  font-size: 14px; color: #262626;
  border-left: 3px solid transparent;
}
.learn-tree-row:hover { background: #FFF5F7; border-left-color: #ffb3c1; }
.learn-tree-row.active {
  background: #FFF5F7; color: #FF2442; font-weight: 600;
  border-left-color: #FF2442;
}
/* 顶级分类：加粗 + 稍大 + 更深色 */
.learn-tree-row.top-level {
  font-weight: 600;
  font-size: 15px;
  color: #1a1a1a;
  padding-top: 10px;
  padding-bottom: 10px;
}
/* 顶级分类之间间距 */
.learn-tree-node + .learn-tree-node > .learn-tree-row.top-level {
  margin-top: 4px;
}
/* 子级分类：常规字重 + 稍小 + 稍浅色 + 层级竖线 */
.learn-tree-row.child-level {
  font-weight: 400;
  font-size: 13px;
  color: #595959;
  border-left: 3px solid #f0f0f0;
}
.learn-tree-row.child-level:hover { border-left-color: #ffb3c1; }
.learn-tree-row.child-level.active {
  color: #FF2442;
  font-weight: 600;
  border-left-color: #FF2442;
}
.learn-tree-caret {
  width: 14px; text-align: center; font-size: 12px; color: #bfbfbf;
  flex-shrink: 0;
}
.learn-tree-caret-spacer { width: 14px; display: inline-block; flex-shrink: 0; }
.learn-tree-icon {
  width: 16px; height: 16px; font-size: 16px; color: #8c8c8c;
  flex-shrink: 0;
}
.learn-tree-row:hover .learn-tree-icon,
.learn-tree-row.active .learn-tree-icon { color: #FF2442; }
.learn-tree-dot {
  width: 4px; height: 4px; border-radius: 50%;
  background: #FF2442; flex-shrink: 0;
}
.learn-tree-children { list-style: none; padding: 0; margin: 0; }
```

- [ ] **Step 5: 修改 `LearnIndex.vue` —— Hero 模板**

在 `<NavBar ... />` 之后、`<div class="learn-body">` 之前插入：

```vue
    <header class="learn-hero">
      <div class="learn-hero-deco learn-hero-deco-lg"></div>
      <div class="learn-hero-deco learn-hero-deco-sm"></div>
      <div class="learn-hero-inner">
        <h1 class="learn-hero-title">创作学院</h1>
        <p class="learn-hero-subtitle">从 0 到 1 的自媒体实战指南</p>
      </div>
    </header>
```

- [ ] **Step 6: 修改 `LearnIndex.vue` —— Hero 样式**

在 `<style scoped>` 中 `.learn-page` 规则之后追加：

```css
/* Hero 区 */
.learn-hero {
  position: relative;
  background: linear-gradient(180deg, #FFF5F7 0%, #FFFFFF 100%);
  padding: 32px 24px;
  overflow: hidden;
}
.learn-hero-inner {
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}
.learn-hero-title {
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}
.learn-hero-subtitle {
  font-size: 14px;
  color: #8c8c8c;
  margin: 8px 0 0;
}
.learn-hero-deco {
  position: absolute;
  border-radius: 50%;
  background: #FFE8EC;
}
.learn-hero-deco-lg {
  width: 200px; height: 200px;
  top: -60px; right: -40px;
}
.learn-hero-deco-sm {
  width: 80px; height: 80px;
  top: 20px; right: 160px;
}
@media (max-width: 991px) {
  .learn-hero { padding: 20px 16px; }
  .learn-hero-title { font-size: 24px; }
}
```

- [ ] **Step 7: 截图验证**

```bash
python3 -c "
from playwright.sync_api import sync_playwright
import time
with sync_playwright() as p:
    b = p.chromium.launch()
    pg = b.new_page(viewport={'width':1440,'height':900})
    pg.goto('http://localhost:22345/learn')
    time.sleep(1.5)
    pg.screenshot(path='tests/e2e/screenshots/task1-hero-sidebar.png', full_page=True)
    b.close()
    print('OK')
"
```

预期：截图中能看到 Hero 区（淡红渐变 + `创作学院` 标题）、侧边栏顶级分类有 SVG 图标、折叠图标是 `›/∨`。

- [ ] **Step 8: 提交**

```bash
git add project/user/web/src/components/learn/learnCategoryIcons.js \
        project/user/web/src/components/learn/LearnSidebarNode.vue \
        project/user/web/src/views/LearnIndex.vue
git commit -m "feat(user-web): 创作学院 Hero 区 + 侧边栏图标升级

- 新增 Hero 区：淡红渐变 + 装饰几何 + 标题/副标题
- 侧边栏顶级分类加 Ant Design 线性图标（AimOutlined 等 6 个）
- 折叠图标 +/− 换成 ›/∨
- 子级分类加层级竖线，选中态加红色圆点
- hover 加浅红色左边线反馈

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 2: 面包屑 + 文章元信息条

**Files:**
- Modify: `project/user/web/src/views/LearnIndex.vue`
- Modify: `project/user/web/src/components/learn/LearnContent.vue`

**Interfaces:**
- Consumes: 现有 `currentArticle`、`categoryTree`。
- Produces:
  - `LearnIndex.vue` 新增 `currentCategoryPath` computed，传入 `LearnContent`
  - `LearnContent.vue` 新增 prop `categoryPath: Array`
  - `LearnContent.vue` 渲染面包屑 + 元信息条

- [ ] **Step 1: 修改 `LearnIndex.vue` —— 新增 `currentCategoryPath`**

在 `currentCategoryName` computed 之后追加：

```js
// 反查当前文章所属分类的完整路径（用于面包屑）
const currentCategoryPath = computed(() => {
  if (!currentArticle.value?.categoryId) return []
  const targetId = currentArticle.value.categoryId
  const result = []
  const walk = (nodes, trail) => {
    for (const n of nodes) {
      const current = [...trail, { id: n.id, name: n.name }]
      if (n.id === targetId) {
        result.push(...current)
        return true
      }
      if (n.children?.length && walk(n.children, current)) return true
    }
    return false
  }
  walk(categoryTree.value, [])
  return result
})
```

- [ ] **Step 2: 修改 `LearnIndex.vue` —— 传 `category-path` prop**

把 `<LearnContent>` 调用改为：

```vue
        <LearnContent
          :article="currentArticle"
          :category="currentCategory"
          :current-category-name="currentCategoryName"
          :category-path="currentCategoryPath"
          @load-article="loadArticle"
        />
```

- [ ] **Step 3: 修改 `LearnContent.vue` —— 新增 props 与 computed**

把 `<script setup>` 改为：

```js
import { computed } from 'vue'
import LearnMarkdown from './LearnMarkdown.vue'
import LearnRichText from './LearnRichText.vue'
import {
  CalendarOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
  TagOutlined
} from '@ant-design/icons-vue'

const props = defineProps({
  article: { type: Object, default: null },
  category: { type: Object, default: null },
  currentCategoryName: { type: String, default: '' },
  categoryPath: { type: Array, default: () => [] }
})
defineEmits(['load-article'])

const readingMinutes = computed(() => {
  if (!props.article?.content) return 0
  return Math.max(1, Math.ceil(props.article.content.length / 300))
})

const wordCount = computed(() => props.article?.content?.length || 0)

function formatDate(d) {
  if (!d) return ''
  const dt = new Date(d)
  return `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')}`
}
```

- [ ] **Step 4: 修改 `LearnContent.vue` —— 面包屑 + 元信息条模板**

把 `<!-- 文章详情 -->` 那一段的 `<header class="learn-content-head">` 改为：

```vue
      <header class="learn-content-head">
        <!-- 面包屑 -->
        <nav v-if="categoryPath.length" class="learn-breadcrumb">
          <router-link to="/learn" class="learn-breadcrumb-item">创作学院</router-link>
          <template v-for="(seg, i) in categoryPath" :key="seg.id">
            <span class="learn-breadcrumb-sep">›</span>
            <router-link
              :to="`/learn?cat=${seg.id}`"
              class="learn-breadcrumb-item"
              :class="{ active: i === categoryPath.length - 1 }"
            >{{ seg.name }}</router-link>
          </template>
        </nav>

        <h1 class="learn-content-title">{{ article.title }}</h1>
        <p v-if="article.summary" class="learn-content-summary">{{ article.summary }}</p>

        <!-- 元信息条 -->
        <div class="learn-meta-bar">
          <span class="learn-meta-item">
            <CalendarOutlined class="learn-meta-icon" />
            {{ formatDate(article.publishedAt || article.updatedAt) }}
          </span>
          <span class="learn-meta-item">
            <ClockCircleOutlined class="learn-meta-icon" />
            约 {{ readingMinutes }} 分钟
          </span>
          <span class="learn-meta-item">
            <FileTextOutlined class="learn-meta-icon" />
            {{ wordCount }} 字
          </span>
          <router-link
            v-if="currentCategoryName"
            :to="`/learn?cat=${article.categoryId}`"
            class="learn-meta-tag"
          >
            <TagOutlined class="learn-meta-icon" />
            {{ currentCategoryName }}
          </router-link>
        </div>
      </header>
```

- [ ] **Step 5: 修改 `LearnContent.vue` —— 面包屑 + 元信息条样式**

在 `<style scoped>` 中 `.learn-content-head` 规则之后追加：

```css
/* 面包屑 */
.learn-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.learn-breadcrumb-item {
  color: #8c8c8c;
  text-decoration: none;
}
.learn-breadcrumb-item:hover { color: #FF2442; }
.learn-breadcrumb-item.active { color: #262626; font-weight: 600; }
.learn-breadcrumb-sep { color: #d9d9d9; }

/* 元信息条 */
.learn-meta-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #8c8c8c;
}
.learn-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.learn-meta-icon { font-size: 14px; }
.learn-meta-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  background: #FFF5F7;
  color: #FF2442;
  border-radius: 9999px;
  font-size: 12px;
  text-decoration: none;
}
.learn-meta-tag:hover { background: #FFE8EC; }
```

- [ ] **Step 6: 截图验证**

```bash
python3 -c "
from playwright.sync_api import sync_playwright
import time
with sync_playwright() as p:
    b = p.chromium.launch()
    pg = b.new_page(viewport={'width':1440,'height':900})
    pg.goto('http://localhost:22345/learn/article/3')
    time.sleep(1.5)
    pg.screenshot(path='tests/e2e/screenshots/task2-breadcrumb-meta.png', full_page=True)
    b.close()
    print('OK')
"
```

预期：截图中文章标题上方有面包屑（`创作学院 › ... › ...`），标题下方有元信息条（日期、阅读时长、字数、分类 chip）。

- [ ] **Step 7: 提交**

```bash
git add project/user/web/src/views/LearnIndex.vue \
        project/user/web/src/components/learn/LearnContent.vue
git commit -m "feat(user-web): 创作学院面包屑 + 文章元信息条

- 文章详情页顶部加面包屑（创作学院 › 父分类 › 当前分类），可点击跳转
- 标题下方加元信息条：发布日期、阅读时长、字数、分类标签 chip
- 阅读时长按 content.length / 300 估算
- 分类标签 chip 点击跳对应分类页

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 3: 上下篇卡片 + CTA 升级

**Files:**
- Modify: `project/user/web/src/components/learn/LearnContent.vue`

**Interfaces:**
- Consumes: 现有 `article.prevArticle` / `article.nextArticle` / `currentCategoryName`。
- Produces: 升级后的上下篇卡片样式 + CTA 卡片。

- [ ] **Step 1: 修改 `LearnContent.vue` —— 上下篇卡片模板**

把现有的 `<nav v-if="article.prevArticle || article.nextArticle" class="learn-nav">` 整段替换为：

```vue
      <nav v-if="article.prevArticle || article.nextArticle" class="learn-nav">
        <router-link
          v-if="article.prevArticle"
          :to="`/learn/article/${article.prevArticle.id}`"
          class="learn-nav-card learn-nav-prev"
        >
          <span class="learn-nav-dir">← 上一篇</span>
          <span
            v-if="currentCategoryName && article.prevArticle.categoryName !== currentCategoryName"
            class="learn-nav-cat-chip"
          >{{ article.prevArticle.categoryName }}</span>
          <span class="learn-nav-title">{{ article.prevArticle.title }}</span>
        </router-link>

        <router-link
          v-if="article.nextArticle"
          :to="`/learn/article/${article.nextArticle.id}`"
          class="learn-nav-card learn-nav-next"
        >
          <span class="learn-nav-dir">下一篇 →</span>
          <span
            v-if="currentCategoryName && article.nextArticle.categoryName !== currentCategoryName"
            class="learn-nav-cat-chip"
          >{{ article.nextArticle.categoryName }}</span>
          <span class="learn-nav-title">{{ article.nextArticle.title }}</span>
        </router-link>
      </nav>
```

- [ ] **Step 2: 修改 `LearnContent.vue` —— CTA 模板**

把现有的 `<footer class="learn-content-foot">` 整段替换为：

```vue
      <footer class="learn-content-foot">
        <div class="learn-cta-card">
          <BulbOutlined class="learn-cta-icon" />
          <div class="learn-cta-text">
            <div class="learn-cta-title">想把自己的账号也做成这样？</div>
            <div class="learn-cta-subtitle">用 AI 一分钟生成你的第一篇</div>
          </div>
          <router-link to="/login" class="learn-cta-btn">立即开始创作 →</router-link>
        </div>
      </footer>
```

- [ ] **Step 3: 修改 `LearnContent.vue` —— 新增 BulbOutlined import**

在 `<script setup>` 的 import 区域追加：

```js
import { BulbOutlined } from '@ant-design/icons-vue'
```

（与 Task 2 的 icons import 合并到同一个 import 语句中。）

- [ ] **Step 4: 修改 `LearnContent.vue` —— 上下篇卡片 + CTA 样式**

把现有的 `/* 上下篇导航 */` 样式段替换为：

```css
/* 上下篇导航 */
.learn-nav {
  display: flex;
  gap: 12px;
  margin: 32px 0;
}
.learn-nav-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 20px;
  border: 1px solid #eee;
  border-radius: 12px;
  background: #fff;
  color: #1a1a1a;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 0;
}
.learn-nav-card:hover {
  border-color: #FF2442;
  box-shadow: 0 4px 12px rgba(255, 36, 66, 0.12);
  transform: translateY(-2px);
}
.learn-nav-card:hover .learn-nav-title { color: #FF2442; }
.learn-nav-prev { text-align: left; align-items: flex-start; }
.learn-nav-next { text-align: right; align-items: flex-end; }
.learn-nav-dir {
  font-size: 12px;
  color: #8c8c8c;
  font-weight: 500;
}
.learn-nav-card:hover .learn-nav-dir { color: #FF2442; }
.learn-nav-cat-chip {
  display: inline-block;
  padding: 2px 10px;
  background: #FFF5F7;
  color: #FF2442;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 400;
}
.learn-nav-title {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-width: 100%;
  color: #1a1a1a;
}
@media (max-width: 991px) {
  .learn-nav { flex-direction: column; }
  .learn-nav-prev,
  .learn-nav-next { text-align: left; align-items: flex-start; }
}

/* CTA 卡片 */
.learn-content-foot {
  border-top: none;
  padding-top: 0;
  text-align: left;
}
.learn-cta-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px 28px;
  background: linear-gradient(135deg, #FFF5F7 0%, #FFE8EC 100%);
  border-radius: 12px;
}
.learn-cta-icon {
  font-size: 32px;
  color: #FF2442;
  flex-shrink: 0;
}
.learn-cta-text { flex: 1; min-width: 0; }
.learn-cta-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
.learn-cta-subtitle {
  font-size: 13px;
  color: #8c8c8c;
  margin-top: 4px;
}
.learn-cta-btn {
  display: inline-block;
  padding: 10px 24px;
  background: #FF2442;
  color: #fff;
  border-radius: 9999px;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  white-space: nowrap;
  transition: background 0.2s;
}
.learn-cta-btn:hover { background: #e61e3a; }
@media (max-width: 991px) {
  .learn-cta-card { flex-direction: column; text-align: center; }
  .learn-cta-btn { width: 100%; text-align: center; }
}
```

- [ ] **Step 5: 清理旧样式**

删除以下不再使用的样式规则（如果还存在）：
- `.learn-cta` 和 `.learn-cta:hover`（旧 CTA 纯文字样式）
- `.learn-nav-cat`（旧分类名小字样式，已被 `.learn-nav-cat-chip` 替代）

- [ ] **Step 6: 截图验证**

```bash
python3 -c "
from playwright.sync_api import sync_playwright
import time
with sync_playwright() as p:
    b = p.chromium.launch()
    pg = b.new_page(viewport={'width':1440,'height':900})
    pg.goto('http://localhost:22345/learn/article/3')
    time.sleep(1.5)
    pg.evaluate('window.scrollTo(0, document.body.scrollHeight)')
    time.sleep(0.5)
    pg.screenshot(path='tests/e2e/screenshots/task3-nav-cta.png', full_page=True)
    b.close()
    print('OK')
"
```

预期：截图底部能看到升级后的上下篇卡片（圆角 12px、hover 有抬起效果）和 CTA 卡片（浅红渐变背景 + 灯泡图标 + 红色胶囊按钮）。

- [ ] **Step 7: 提交**

```bash
git add project/user/web/src/components/learn/LearnContent.vue
git commit -m "feat(user-web): 创作学院上下篇卡片 + CTA 升级

- 上下篇卡片：圆角 12px、hover 抬起 + 阴影、跨分类显示分类 chip
- CTA 升级成品牌卡：浅红渐变背景 + BulbOutlined 图标 + 标题副标题 + 红色胶囊按钮
- 清理旧 .learn-cta / .learn-nav-cat 样式

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 4: 空状态 + 分类列表卡片化

**Files:**
- Modify: `project/user/web/src/views/LearnIndex.vue`
- Modify: `project/user/web/src/components/learn/LearnContent.vue`

**Interfaces:**
- Consumes: 现有 `categoryTree`、`currentCategory`。
- Produces:
  - `LearnIndex.vue` 新增 `topCategories` computed，传入 `LearnContent`
  - `LearnContent.vue` 新增 prop `topCategories: Array`
  - `LearnContent.vue` 渲染空状态（装饰图标 + 快捷入口 chip）+ 分类列表文章卡片

- [ ] **Step 1: 修改 `LearnIndex.vue` —— 新增 `topCategories`**

在 `currentCategoryPath` computed 之后追加：

```js
// 空状态快捷入口：前 4 个顶级分类
const topCategories = computed(() => categoryTree.value.slice(0, 4))
```

- [ ] **Step 2: 修改 `LearnIndex.vue` —— 传 `top-categories` prop**

把 `<LearnContent>` 调用改为：

```vue
        <LearnContent
          :article="currentArticle"
          :category="currentCategory"
          :current-category-name="currentCategoryName"
          :category-path="currentCategoryPath"
          :top-categories="topCategories"
          @load-article="loadArticle"
          @select-category="onSelectCategory"
        />
```

- [ ] **Step 3: 修改 `LearnContent.vue` —— 新增 props 与 emit**

在 `defineProps` 中追加 `topCategories`：

```js
const props = defineProps({
  article: { type: Object, default: null },
  category: { type: Object, default: null },
  currentCategoryName: { type: String, default: '' },
  categoryPath: { type: Array, default: () => [] },
  topCategories: { type: Array, default: () => [] }
})
```

把 `defineEmits` 改为：

```js
defineEmits(['load-article', 'select-category'])
```

- [ ] **Step 4: 修改 `LearnContent.vue` —— 分类列表模板（含面包屑 + 卡片）**

把 `<!-- 分类详情（列表） -->` 那一段替换为：

```vue
    <!-- 分类详情（列表） -->
    <template v-else-if="category && category.articles && category.articles.length">
      <header class="learn-content-head">
        <nav v-if="categoryPath.length" class="learn-breadcrumb">
          <router-link to="/learn" class="learn-breadcrumb-item">创作学院</router-link>
          <template v-for="(seg, i) in categoryPath" :key="seg.id">
            <span class="learn-breadcrumb-sep">›</span>
            <router-link
              :to="`/learn?cat=${seg.id}`"
              class="learn-breadcrumb-item"
              :class="{ active: i === categoryPath.length - 1 }"
            >{{ seg.name }}</router-link>
          </template>
        </nav>
        <h1 class="learn-content-title">{{ category.name }}</h1>
        <p class="learn-content-count">本分类下共 {{ category.total || category.articles.length }} 篇文章</p>
      </header>
      <ul class="learn-article-list">
        <li v-for="a in category.articles" :key="a.id" class="learn-article-card">
          <a @click.prevent="$emit('load-article', a.id)" href="#" class="learn-article-card-link">
            <div class="learn-article-card-title">{{ a.title }}</div>
            <p v-if="a.summary" class="learn-article-card-summary">{{ a.summary }}</p>
            <div class="learn-article-card-meta">
              <span class="learn-meta-item">
                <CalendarOutlined class="learn-meta-icon" />
                {{ formatDate(a.publishedAt || a.updatedAt) }}
              </span>
            </div>
          </a>
        </li>
      </ul>
    </template>
```

- [ ] **Step 5: 修改 `LearnContent.vue` —— 空分类与空状态模板**

把 `<template v-else-if="category">` 和 `<template v-else>` 两段替换为：

```vue
    <template v-else-if="category">
      <header class="learn-content-head">
        <h1 class="learn-content-title">{{ category.name }}</h1>
      </header>
      <div class="learn-content-empty">
        <ReadOutlined class="learn-empty-icon" />
        <div class="learn-empty-title">该分类下暂无已发布文章</div>
      </div>
    </template>

    <template v-else>
      <div class="learn-content-empty">
        <ReadOutlined class="learn-empty-icon" />
        <div class="learn-empty-title">欢迎来到创作学院</div>
        <div class="learn-empty-subtitle">从左侧选择一个分类开始学习</div>
        <div v-if="topCategories.length" class="learn-empty-chips">
          <a
            v-for="cat in topCategories"
            :key="cat.id"
            class="learn-empty-chip"
            @click.prevent="$emit('select-category', cat.id)"
            href="#"
          >{{ cat.name }}</a>
        </div>
      </div>
    </template>
```

- [ ] **Step 6: 修改 `LearnContent.vue` —— 新增 ReadOutlined import**

在 `<script setup>` 的 icons import 中追加 `ReadOutlined`：

```js
import {
  CalendarOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
  TagOutlined,
  BulbOutlined,
  ReadOutlined
} from '@ant-design/icons-vue'
```

- [ ] **Step 7: 修改 `LearnContent.vue` —— 分类列表卡片 + 空状态样式**

在 `<style scoped>` 中，把现有的 `.learn-article-list` / `.learn-article-item` 相关样式替换为：

```css
/* 分类标题区 */
.learn-content-count {
  font-size: 13px;
  color: #8c8c8c;
  margin: 8px 0 0;
}

/* 文章卡片列表 */
.learn-article-list { list-style: none; margin: 0; padding: 0; }
.learn-article-card {
  margin-bottom: 12px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fff;
  transition: all 0.2s ease;
}
.learn-article-card:hover {
  border-color: #FF2442;
  box-shadow: 0 2px 8px rgba(255, 36, 66, 0.08);
}
.learn-article-card-link {
  display: block;
  padding: 16px 20px;
  text-decoration: none;
  color: inherit;
}
.learn-article-card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
.learn-article-card:hover .learn-article-card-title { color: #FF2442; }
.learn-article-card-summary {
  font-size: 14px;
  color: #595959;
  margin: 6px 0 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.learn-article-card-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #8c8c8c;
}

/* 空状态 */
.learn-content-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  gap: 12px;
  color: #8c8c8c;
  text-align: center;
}
.learn-empty-icon {
  font-size: 64px;
  color: #FFE8EC;
}
.learn-empty-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}
.learn-empty-subtitle {
  font-size: 14px;
  color: #8c8c8c;
}
.learn-empty-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
  margin-top: 8px;
}
.learn-empty-chip {
  display: inline-block;
  padding: 8px 16px;
  border: 1px solid #eee;
  border-radius: 9999px;
  background: #fff;
  color: #262626;
  font-size: 14px;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s;
}
.learn-empty-chip:hover {
  border-color: #FF2442;
  color: #FF2442;
}
```

- [ ] **Step 8: 清理旧样式**

删除以下不再使用的样式规则：
- `.learn-article-item` 及其子选择器（`.learn-article-item a`、`.learn-article-item a:hover`、`.learn-article-summary`、`.learn-article-meta`）
- 旧的 `.learn-content-empty`（如果与新写的不一致）

- [ ] **Step 9: 截图验证**

```bash
python3 -c "
from playwright.sync_api import sync_playwright
import time
with sync_playwright() as p:
    b = p.chromium.launch()
    pg = b.new_page(viewport={'width':1440,'height':900})
    # 空状态
    pg.goto('http://localhost:22345/learn')
    time.sleep(1.5)
    pg.screenshot(path='tests/e2e/screenshots/task4-empty.png', full_page=True)
    # 分类列表
    pg.goto('http://localhost:22345/learn?cat=1')
    time.sleep(1.5)
    pg.screenshot(path='tests/e2e/screenshots/task4-category.png', full_page=True)
    b.close()
    print('OK')
"
```

预期：
- `task4-empty.png`：空状态居中，有 `ReadOutlined` 大图标、`欢迎来到创作学院` 标题、副标题、快捷入口 chip
- `task4-category.png`：分类列表有面包屑、文章数、文章条目是卡片样式

- [ ] **Step 10: 提交**

```bash
git add project/user/web/src/views/LearnIndex.vue \
        project/user/web/src/components/learn/LearnContent.vue
git commit -m "feat(user-web): 创作学院空状态 + 分类列表卡片化

- 空状态：ReadOutlined 装饰图标 + 标题副标题 + 前 4 个顶级分类快捷入口 chip
- 分类列表：加面包屑 + 文章数 + 文章条目卡片化（白底圆角 hover 红边阴影）
- 空分类：居中提示 + ReadOutlined 图标

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 5: e2e 验证脚本

**Files:**
- Create: `tests/e2e/learn_ui_polish.py`

**Interfaces:**
- Consumes: 运行中的 user-web（`http://localhost:22345`）+ user-api（`http://localhost:25050`）。

- [ ] **Step 1: 编写脚本**

```python
#!/usr/bin/env python3
"""用户端 - 创作学院 UI 中度打磨端到端验证。

前置条件：
- user-api 启动（默认 25050）
- user-web dev 启动（默认 http://localhost:22345）
- 已通过管理端录入至少 2 个分类、跨分类的 3 篇已发布文章
"""

import os
import sys
import time
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

USER_URL = os.environ.get("USER_URL", "http://localhost:22345")
SCREENSHOTS_DIR = Path(__file__).parent / "screenshots" / "learn_ui_polish"
SCREENSHOTS_DIR.mkdir(parents=True, exist_ok=True)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()

        # ===== Desktop =====
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()

        # 1. Hero 区
        page.goto(f"{USER_URL}/learn")
        time.sleep(1.5)
        expect(page.locator('.learn-hero-title')).to_have_text('创作学院')
        expect(page.locator('.learn-hero-subtitle')).to_be_visible()
        expect(page.locator('.learn-hero-deco-lg')).to_be_visible()
        page.screenshot(path=SCREENSHOTS_DIR / "01-hero.png", full_page=True)

        # 2. 侧边栏图标
        top_nodes = page.locator('.learn-tree-row.top-level')
        assert top_nodes.count() > 0, "no top-level categories"
        first_top = top_nodes.first
        # 顶级分类应有 SVG 图标（Ant Design icon 渲染为 svg）
        svg_count = first_top.locator('svg').count()
        assert svg_count > 0, "top-level category should have icon"
        # 折叠图标应为 › 或 ∨
        caret = first_top.locator('.learn-tree-caret')
        if caret.count() > 0:
            text = caret.inner_text()
            assert text in ('›', '∨'), f"caret should be › or ∨, got {text}"

        # 3. 空状态
        expect(page.locator('.learn-empty-title')).to_have_text('欢迎来到创作学院')
        expect(page.locator('.learn-empty-subtitle')).to_be_visible()
        chips = page.locator('.learn-empty-chip')
        assert chips.count() > 0, "empty state should have quick access chips"

        # 4. 分类列表（点击第一个顶级分类）
        top_nodes.first.click()
        time.sleep(1.0)
        page.screenshot(path=SCREENSHOTS_DIR / "02-category-list.png", full_page=True)
        # 面包屑
        breadcrumb = page.locator('.learn-breadcrumb')
        if breadcrumb.count() > 0:
            expect(breadcrumb).to_be_visible()
        # 文章卡片
        cards = page.locator('.learn-article-card')
        if cards.count() > 0:
            first_card = cards.first
            expect(first_card.locator('.learn-article-card-title')).to_be_visible()

        # 5. 文章详情：面包屑 + 元信息条 + 上下篇 + CTA
        page.goto(f"{USER_URL}/learn/article/3")
        time.sleep(1.5)
        page.screenshot(path=SCREENSHOTS_DIR / "03-article-top.png", full_page=True)

        # 面包屑
        expect(page.locator('.learn-breadcrumb')).to_be_visible()
        # 元信息条
        expect(page.locator('.learn-meta-bar')).to_be_visible()
        expect(page.locator('.learn-meta-item').first).to_be_visible()
        # 阅读时长
        meta_text = page.locator('.learn-meta-bar').inner_text()
        assert '分钟' in meta_text, f"meta bar should contain reading minutes, got: {meta_text}"

        # 滚到底部看上下篇 + CTA
        page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        time.sleep(0.5)
        page.screenshot(path=SCREENSHOTS_DIR / "04-article-bottom.png", full_page=True)

        # 上下篇卡片
        nav_cards = page.locator('.learn-nav-card')
        assert nav_cards.count() > 0, "article should have prev/next cards"
        # CTA 卡片
        expect(page.locator('.learn-cta-card')).to_be_visible()
        expect(page.locator('.learn-cta-btn')).to_be_visible()
        cta_text = page.locator('.learn-cta-btn').inner_text()
        assert '立即开始创作' in cta_text, f"CTA button text wrong: {cta_text}"

        ctx.close()

        # ===== Mobile =====
        ctx2 = browser.new_context(viewport={"width": 390, "height": 800})
        page2 = ctx2.new_page()
        page2.goto(f"{USER_URL}/learn/article/3")
        time.sleep(1.5)
        page2.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        time.sleep(0.5)
        page2.screenshot(path=SCREENSHOTS_DIR / "05-mobile-article.png", full_page=True)
        # 移动端 CTA 按钮应全宽
        cta_btn = page2.locator('.learn-cta-btn')
        box = cta_btn.bounding_box()
        assert box['width'] > 300, f"mobile CTA button should be wide, got {box['width']}"
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

确保 user-api 与 user-web 都已启动。

```bash
python3 tests/e2e/learn_ui_polish.py
```

预期：输出 `OK screenshots -> tests/e2e/screenshots/learn_ui_polish`，目录下生成 5 张截图。

- [ ] **Step 3: 提交**

```bash
git add tests/e2e/learn_ui_polish.py
git commit -m "test(e2e): 创作学院 UI 打磨验证脚本

- 覆盖 Hero、侧边栏图标、空状态、分类列表卡片、面包屑、
  元信息条、上下篇卡片、CTA 卡片、移动端堆叠

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 自检结果

- **Spec 覆盖**：
  - Hero 区 → Task 1 Step 5-6
  - 侧边栏图标 + `›/∨` + 层级竖线 + 选中圆点 → Task 1 Step 1-4
  - 面包屑 → Task 2 Step 1-5
  - 元信息条（日期/时长/字数/分类 chip） → Task 2 Step 3-5
  - 上下篇卡片升级 → Task 3 Step 1-4
  - CTA 卡片 → Task 3 Step 2-4
  - 空状态 → Task 4 Step 5-7
  - 分类列表卡片化 → Task 4 Step 4-7
  - e2e → Task 5
- **占位符扫描**：无 TBD/TODO，所有代码块完整。
- **类型一致性**：
  - `categoryPath` 在 `LearnIndex.vue`（computed）→ `LearnContent.vue`（prop `Array`）一致
  - `topCategories` 在 `LearnIndex.vue`（computed）→ `LearnContent.vue`（prop `Array`）一致
  - `CATEGORY_ICONS` 在 `learnCategoryIcons.js`（导出）→ `LearnSidebarNode.vue`（import）一致
  - `learn-nav-cat-chip` / `learn-cta-card` / `learn-empty-chip` 等 class 名在模板与样式中一致
