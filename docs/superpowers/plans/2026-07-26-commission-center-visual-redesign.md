# 约稿中心视觉重做实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重做用户端约稿大厅与详情页的视觉和响应式布局，优先提升电脑端设计感与信息层级，再优化手机端浏览和投稿操作，同时保持管理员发布、用户投稿的平台约稿业务模型不变。

**Architecture:** 只修改用户端两个约稿视图的模板与 scoped CSS，继续复用现有 `useCommission`、`useWorks`、Vue Router 和 Ant Design Vue Modal。列表页负责真实任务/投稿数据的展示与筛选，详情页负责任务说明、投稿状态和文章选择；不新增接口、不把发布任务能力放到用户端。

**Tech Stack:** Vue 3 `<script setup>`、Vue Router 4、Ant Design Vue、现有 CSS 变量和响应式断点、Vite。

## Global Constraints

- 约稿任务只能由管理端管理员发布，用户端不显示发布、编辑或删除任务入口。
- 用户只能从自己已生成完成且字数符合要求的文章中选择投稿。
- 管理员采纳后，奖励全额发放给用户。
- 不新增后端接口，不改变现有任务、投稿、撤回和奖励状态规则。
- 保留现有品牌红 `#ff2442` 与设计系统 CSS 变量。
- 不新增图片资源或第三方 UI 组件。
- 电脑端优先保证任务浏览和详情投稿黄金路径，手机端优化单列浏览和底部固定操作。

---

### Task 1: 重做电脑端约稿大厅

**Files:**
- Modify: `project/user/web/src/views/console/CommissionIndex.vue`
- Reference: `docs/superpowers/specs/2026-07-26-commission-center-visual-design.md`

**Interfaces:**
- Consumes: `useCommission().tasks`, `useCommission().mySubmissions`, `loadTasks`, `loadMySubmissions`。
- Produces: 电脑端“官方约稿任务”概览区、分段导航、状态筛选、双列任务卡和我的投稿状态卡；保留 `goDetail(id)` 路由行为。

- [ ] **Step 1: 保留并核对真实数据计算**

确认 `visibleItems` 仍按 `tab` 在 `tasks` 与 `mySubmissions` 之间切换，`status` 只传给全部任务接口；不得添加本地任务或 localStorage 数据。保留状态文本：`招募中`、`已截止待采纳`、`已完成`、`已流局`，投稿状态文本：`等待采纳`、`已采纳`、`未采纳`、`已撤回`。

- [ ] **Step 2: 增加大厅概览模板**

在列表页顶部使用以下结构，统计均由已有数组计算：

```vue
<section class="commission-hero">
  <div class="hero-copy">
    <span class="eyebrow">OFFICIAL COMMISSION</span>
    <h1>官方约稿任务</h1>
    <p>挑选合适的任务，使用你在爱创作中生成完成的文章参与投稿。稿件采纳后，奖励全额发放。</p>
  </div>
  <div class="hero-orbit" aria-hidden="true"><span></span><span></span></div>
  <div class="hero-stats">
    <div><strong>{{ activeTaskCount }}</strong><span>进行中的任务</span></div>
    <div><strong>{{ mySubmissionCount }}</strong><span>我的投稿</span></div>
    <div><strong>{{ earnedCoinTotal }}</strong><span>已获得创作币</span></div>
  </div>
</section>
```

新增 computed：`activeTaskCount` 统计 status 为 0 的任务；`mySubmissionCount` 为 `mySubmissions.length`；`earnedCoinTotal` 对已采纳 status 为 1 的投稿求和 `rewardCoin`，无值时按 0 处理。

- [ ] **Step 3: 将导航与筛选改为大厅控件**

将现有普通下划线 Tab 改为 `commission-switcher` 胶囊分段控件；筛选按钮使用 `filter-chip`。全部任务保留 5 个状态选项，切换后继续调用 `refresh()`，不增加用户发布按钮。

- [ ] **Step 4: 实现双列任务卡模板**

全部任务卡使用状态、奖励、标题、描述、字数、计划/已采纳数量、截止时间和详情箭头：

```vue
<article v-for="item in visibleItems" :key="item.id" class="task-card" @click="goDetail(item.taskId || item.id)">
  <div class="task-card-top">
    <span :class="['status-tag', `status-${item.status}`]">{{ taskStatus(item.status) }}</span>
    <strong class="task-reward">{{ item.rewardCoin }}<small> 创作币 / 篇</small></strong>
  </div>
  <h2>{{ item.taskTitle || item.title || `任务 #${item.taskId}` }}</h2>
  <p class="task-desc">{{ item.description }}</p>
  <div class="task-facts">
    <span>{{ item.minWordCount }}-{{ item.maxWordCount }} 字</span>
    <span>采纳 {{ item.adoptedCount }}/{{ item.neededCount }} 篇</span>
  </div>
  <div class="task-card-bottom"><span>{{ deadlineText(item) }}</span><span class="detail-link">查看详情 <span>→</span></span></div>
</article>
```

我的投稿视图使用独立 `submission-card` 模板，展示任务标题、文章标题/字数、投稿状态、奖励结果和详情入口，不展示任务发布操作。

- [ ] **Step 5: 编写大厅 scoped CSS**

实现以下电脑端规则：最大宽度 1120px；概览区使用淡红/淡紫渐变、24px 圆角和 CSS 圆形装饰；任务卡使用双列 grid、20px 圆角、轻阴影和 hover 上移；奖励金额为主色大字号；状态色分别为蓝、橙、绿、灰；描述最多 2 行；卡片按钮和文字保持可读字号。

- [ ] **Step 6: 构建用户端并检查模板错误**

运行：

```bash
npm run build --prefix project/user/web
```

预期：Vite 构建成功，无 Vue 模板、CSS 或 TypeScript/JavaScript 编译错误。

---

### Task 2: 重做任务详情页电脑端

**Files:**
- Modify: `project/user/web/src/views/console/CommissionDetail.vue`

**Interfaces:**
- Consumes: `taskDetail`, `loadTask`, `submitArticle`, `withdrawSubmission`, `useWorks().articles`。
- Produces: 左侧任务说明、右侧奖励与投稿操作卡；保留现有投稿、撤回和文章选择调用。

- [ ] **Step 1: 调整详情模板为双栏结构**

保留当前 `loading`、任务不存在和 Modal 分支。任务存在时使用：

```vue
<div class="detail-shell">
  <button class="back-link" @click="router.push('/console/commission')">← 返回约稿大厅</button>
  <div class="detail-grid">
    <main class="detail-main">
      <div class="detail-status-line"><span :class="['status-tag', `status-${task.status}`]">{{ taskStatus(task.status) }}</span><span class="official-label">平台官方发布</span></div>
      <h1>{{ task.title }}</h1>
      <p class="detail-lead">使用爱创作生成完成的文章参与投稿，稿件采纳后奖励全额发放。</p>
      <section class="detail-section"><h2>任务说明</h2><div class="description">{{ task.description }}</div></section>
      <section class="detail-section"><h2>投稿要求</h2><div class="requirement-grid"><div><span>字数范围</span><strong>{{ task.minWordCount }}-{{ task.maxWordCount }} 字</strong></div><div><span>计划采纳</span><strong>{{ task.neededCount }} 篇</strong></div></div></section>
      <section v-if="task.styleHint" class="detail-section"><h2>风格提示</h2><p class="style-hint">{{ task.styleHint }}</p></section>
    </main>
    <aside class="action-card">...</aside>
  </div>
</div>
```

操作卡根据 `mySubmission` 和 `task.status` 展示投稿按钮、已投稿状态、已采纳奖励到账提示或不可投稿状态。

- [ ] **Step 2: 增强投稿文章选择 Modal 内容**

文章项继续禁用不符合字数的文章；在现有标题和字数旁补充 `article.platform`、`article.completedAt`（有值才显示），并显示“字数不符合要求”原因。Modal 底部保留取消和确认投稿按钮，不能选择空文章。

- [ ] **Step 3: 编写详情页电脑端 CSS**

实现 `detail-grid` 左侧自适应、右侧 300px 固定宽度；操作卡在桌面端 `position: sticky; top: 24px`；奖励金额 32px；任务说明、要求和风格提示使用分段面板；所有按钮最小高度 40px；沿用明暗主题变量。

- [ ] **Step 4: 构建并检查详情页行为代码**

运行：

```bash
npm run build --prefix project/user/web
```

预期：构建成功，保留 `/console/commission/:id` 路由、投稿、撤回和 Modal 关闭行为。

---

### Task 3: 优化手机端大厅与详情操作

**Files:**
- Modify: `project/user/web/src/views/console/CommissionIndex.vue`
- Modify: `project/user/web/src/views/console/CommissionDetail.vue`

**Interfaces:**
- Consumes: Task 1 和 Task 2 产生的模板 class 与状态逻辑。
- Produces: 768px 以下单列卡片、横向指标、详情垂直流和固定底部投稿栏。

- [ ] **Step 1: 添加手机端大厅媒体查询**

在 `@media (max-width: 768px)` 中将任务 grid 改为单列；概览区降为纵向布局；统计区域使用 `display:flex; overflow-x:auto`；隐藏装饰的非必要部分；任务卡内边距 16px，描述最多两行，奖励固定在右上区域；保证卡片点击区域完整。

- [ ] **Step 2: 添加手机端详情固定操作栏**

详情页在任务存在且用户可投稿时渲染：

```vue
<div v-if="canSubmit" class="mobile-submit-bar">
  <span>仅支持平台生成文章</span>
  <button class="primary-btn" @click="openPicker">选择文章投稿</button>
</div>
```

`canSubmit` 必须等价于 `task.status === 0 && !mySubmission`。为详情内容增加底部 padding，避免固定栏遮挡最后内容；桌面端隐藏该栏。

- [ ] **Step 3: 编写手机端详情媒体查询**

768px 以下改为单栏，隐藏桌面 sticky 规则；详情标题字号 24px；操作卡改为普通内容卡；状态和指标允许换行；按钮宽度可用但不超过容器；Modal 宽度使用 `min(560px, calc(100vw - 24px))`；固定栏支持明暗主题。

- [ ] **Step 4: 运行用户端构建**

运行：

```bash
npm run build --prefix project/user/web
```

预期：构建成功。

---

### Task 4: 浏览器验证电脑端与手机端黄金路径

**Files:**
- Modify: none
- Test: existing browser/dev-server verification; no new committed test file unless an existing Playwright harness requires it

**Interfaces:**
- Consumes: Tasks 1-3 的最终页面。
- Produces: 电脑端与手机端验证结果和回归检查。

- [ ] **Step 1: 启动用户端开发服务**

运行用户端已有开发命令，确认开发服务可访问；不要修改后端或使用 localStorage 伪造任务。

- [ ] **Step 2: 验证电脑端大厅**

使用真实登录会话打开 `/console/commission`，检查：概览统计、全部任务、状态筛选、双列卡片、我投稿的视图。确认页面没有“发布任务”按钮。

- [ ] **Step 3: 验证电脑端详情投稿**

打开任务详情，检查左侧说明和右侧操作卡；打开文章选择 Modal，验证符合字数文章可选、不符合字数文章禁用，确认投稿后状态更新。

- [ ] **Step 4: 验证手机端大厅与详情**

使用浏览器移动视口检查：单列卡片、统计横向滚动、详情垂直布局、底部固定投稿栏、Modal 不超出屏幕且固定栏不遮挡内容。

- [ ] **Step 5: 回归检查**

确认 Console 导航、作品列表、登录状态和约稿详情返回入口没有回归；再次运行：

```bash
npm run build --prefix project/user/web
```

预期：构建成功，黄金路径无控制台运行时错误。

---

### Task 5: 更新进度记录

**Files:**
- Modify: `.superpowers/sdd/progress.md`

**Interfaces:**
- Consumes: Task 4 验证结果。
- Produces: 约稿中心视觉重做的完成记录。

- [ ] **Step 1: 记录完成范围**

在现有约稿相关进度条目后补充：用户端约稿中心已完成电脑端任务大厅、详情双栏、手机端单列与固定投稿栏视觉优化；任务仍由管理员发布，用户只能投递平台生成文章。

- [ ] **Step 2: 检查文档 diff**

运行：

```bash
git diff --check -- .superpowers/sdd/progress.md
```

预期：无空白错误。
