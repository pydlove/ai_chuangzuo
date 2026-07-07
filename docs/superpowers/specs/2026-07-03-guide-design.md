# 玩法指南（营销/变现说明页）设计

**日期**: 2026-07-03
**状态**: 已确认，待实现
**关联文件**: `project/user/web/src/views/GuideIndex.vue`、`project/user/web/src/views/Home.vue`、`project/user/web/src/router/index.js`、`project/user/web/src/composables/useLeaderboard.js`、`project/user/web/src/views/console/ConsoleLayout.vue`

---

## 1. 功能概述

新增一个公开页面「玩法指南」，采用**左侧分类目录 + 右侧正文内容**的文档式/FAQ 结构，回答用户最关心的两个问题：

1. 这个平台能干嘛？
2. 我通过这个平台能怎么挣钱？

页面同时嵌入两个轻量互动模块：

- **时间节省计算器**：输入每周产量、原耗时、时薪，实时计算每月节省的时间与人工成本。
- **收益排行榜预览**：展示本月创作币榜 TOP 5，作为社会认同，并引导查看完整榜单。

页面面向未登录游客和已登录用户，入口分布在首页导航、首页 Hero 区、控制台新手横幅及相关空状态页面。

---

## 2. 设计决策

- **页面形态**：独立公开页面，路由 `/guide`，不需要登录即可访问。
- **布局模式**：左侧固定分类目录（260px），右侧自适应正文内容区（最大 760px），移动端左侧目录折叠为抽屉。
- **内容组织**：按用户最关心的问题切分大分类：平台能干嘛 → 能赚多少钱 → 怎么赚 → 怎么提现。不放置真实用户案例，避免隐私风险。
- **叙事主线**：三条线分层讲——
  1. 省时间/省人工成本（工具价值）；
  2. 平台内收益（创作币、排行榜、邀请返利等）；
  3. 外部自媒体变现（公众号/小红书/抖音/百家号/头条等）。
- **互动模块**：纯前端实现，时间计算器本地计算；排行榜预览复用 `useLeaderboard`（若已实现）或先用 mock 数据占位。
- **不强制打扰**：不弹强制 modal、不拦截创建流程，只在用户可能产生疑问的节点提供入口。

---

## 3. 页面结构

页面采用**左侧固定目录 + 右侧滚动正文**的两栏布局：

- **顶部**：复用首页导航栏，新增「玩法指南」入口。
- **左侧边栏（260px）**：展示 4 个分类及其下的文章标题，当前阅读项高亮。
- **右侧内容区**：渲染对应分类的标题、正文、互动模块（时间节省计算器、排行榜预览）和 CTA。
- **移动端**：左侧目录折叠为顶部抽屉按钮，正文全宽展示。

整体阅读流：

1. 平台能干嘛 → 2. 能赚多少钱（含时间节省计算器） → 3. 怎么赚（含排行榜预览） → 4. 怎么提现 → 底部 CTA「立即开始创作」。

---

## 4. 左侧目录结构

```
平台能干嘛
  ├─ 爱创作是做什么的
  ├─ 3 分钟能写出什么
  └─ 支持哪些平台

能赚多少钱
  ├─ 平台内收益：创作币、排行榜、邀请
  ├─ 外部自媒体变现：流量主、商单、带货
  └─ 时间节省计算器

怎么赚
  ├─ 第一步：生成第一篇内容
  ├─ 第二步：选择发布平台
  ├─ 第三步：多平台分发
  └─ 第四步：申报收入/冲击榜单

怎么提现
  ├─ 创作币是什么
  ├─ 如何结算收益
  └─ 提现门槛与到账说明
```

点击目录标题后，右侧平滑滚动到对应锚点，URL 同步 `#section-id`，方便分享。

---

## 5. 互动模块

### 5.1 时间节省计算器

**位置**：「能赚多少钱」正文内。

**输入项**：

| 字段 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| 每周写几篇 | 数字输入 | 5 | 正整数 |
| 原来每篇花几小时 | 数字输入 | 2 | 保留 1 位小数，≥0 |
| 时薪估算（元） | 数字输入 | 50 | 保留 0 位小数，≥0 |

**计算逻辑**：

```js
const savedHoursPerWeek = articlesPerWeek * (originalHoursPerArticle - 0.05)
const savedHoursPerMonth = savedHoursPerWeek * 4
const savedMoneyPerMonth = savedHoursPerMonth * hourlyRate
```

> 0.05 小时 ≈ 3 分钟，为爱创作平均成稿时间。

**输出展示**：

- 每月可节省 X 小时
- 相当于 Y 元人工成本
- 下方补一句：“省下的时间可以用来做选题、运营账号，或者直接再生产更多内容。”

**边界**：输入非法（负数、空值、非数字）时，结果区显示“请输入有效数字”。

### 5.2 收益排行榜预览卡片

**位置**：「能赚多少钱」正文内。

**展示内容**：

- 卡片标题：本月创作币榜 TOP 5
- 5 行数据：排名、昵称、头像、金额
- 底部按钮：查看完整榜单

**数据来源**：

- 优先复用 `useLeaderboard.js` 的 `getCoinLeaderboard(currentMonth)`。
- 若 leaderboard 尚未实现，先用固定 mock 数据占位，后续替换。

**跳转逻辑**：

- 已登录用户点击「查看完整榜单」→ `/console/leaderboard`。
- 未登录用户点击→ `/login?from=guide`，登录后再跳转 `/console/leaderboard`。

---

## 6. 入口与引导

### 6.1 首页入口（拉新转化）

- 顶部导航在「会员」旁新增「玩法指南」。
- Hero 区主 CTA 保持「立即开始创作」，主按钮下方加一行小字：
  > “不知道怎么变现？看看玩法指南”

### 6.2 注册/登录后引导（新用户激活）

- 用户进入控制台后，若尚未生成过任何文章，顶部出现一条可关闭的横幅：
  > “新手？3 分钟了解怎么在爱创作变现 →”
- 点击跳转 `/guide`。
- 用户生成过至少一篇文章后，横幅自动消失（通过已有作品数判断）。

### 6.3 相关页面回流

- 「我的账户」收益为空时，空状态文案加链接：
  > “还没有收益，看看怎么赚创作币”
- 「收益排行榜」规则弹框底部加：
  > “阅读完整玩法指南”

### 6.4 不做的事

- 不弹强制 modal。
- 不拦截创建流程。
- 不在每个页面都塞入口。

---

## 7. 组件拆分

| 组件 | 职责 |
|---|---|
| `GuideIndex.vue` | 页面容器，处理布局、主题、响应式、路由参数/锚点 |
| `GuideSidebar.vue` | 左侧目录，接收 `guideSections`，渲染分类与文章树，处理 active 状态 |
| `GuideArticle.vue` | 右侧单篇文章渲染，支持标题、正文、嵌入组件插槽 |
| `TimeCalculator.vue` | 时间节省计算器，纯本地状态 |
| `LeaderboardPreview.vue` | TOP 5 排行榜预览卡片，可复用 leaderboard mock 数据 |

---

## 8. 内容数据结构

```js
const guideSections = [
  {
    id: 'what',
    title: '平台能干嘛',
    articles: [
      { id: 'intro', title: '爱创作是做什么的', content: '...' },
      { id: 'output', title: '3 分钟能写出什么', content: '...' },
      { id: 'platforms', title: '支持哪些平台', content: '...' }
    ]
  },
  {
    id: 'money',
    title: '能赚多少钱',
    articles: [
      { id: 'platform-earnings', title: '平台内收益：创作币、排行榜、邀请', content: '...' },
      { id: 'external-earnings', title: '外部自媒体变现：流量主、商单、带货', content: '...' },
      { id: 'calculator', title: '时间节省计算器', component: 'TimeCalculator' }
    ]
  },
  {
    id: 'how',
    title: '怎么赚',
    articles: [
      { id: 'step1', title: '第一步：生成第一篇内容', content: '...' },
      { id: 'step2', title: '第二步：选择发布平台', content: '...' },
      { id: 'step3', title: '第三步：多平台分发', content: '...' },
      { id: 'step4', title: '第四步：申报收入/冲击榜单', content: '...' },
      { id: 'leaderboard-preview', title: '本月创作币榜', component: 'LeaderboardPreview' }
    ]
  },
  {
    id: 'withdraw',
    title: '怎么提现',
    articles: [
      { id: 'coin', title: '创作币是什么', content: '...' },
      { id: 'settle', title: '如何结算收益', content: '...' },
      { id: 'withdraw-rule', title: '提现门槛与到账说明', content: '...' }
    ]
  }
]
```

文案内容写入 `guide-content.js` 或直接在 `GuideIndex.vue` 中维护，方便运营后续修改。

---

## 9. 路由与导航

### 9.1 新增公开路由

```js
{
  path: '/guide',
  name: 'Guide',
  component: () => import('@/views/GuideIndex.vue')
}
```

### 9.2 首页导航

在 `Home.vue` 顶部导航 `nav-links` 中，于「会员」后新增：

```vue
<router-link to="/guide" class="nav-link">玩法指南</router-link>
```

### 9.3 控制台横幅

在 `ConsoleLayout.vue` 主体内容上方，根据“用户尚未生成过任何文章”的条件渲染可关闭横幅。

---

## 10. 响应式策略

- **PC（≥1024px）**：左侧目录固定 260px，右侧内容区自适应，正文最大宽度 760px。
- **平板（768px–1023px）**：左侧目录宽度缩至 220px，正文 padding 减小。
- **手机（<768px）**：
  - 左侧目录默认隐藏，顶部显示“目录”按钮，点击后从左侧滑出抽屉。
  - 正文全宽，标题字号适当缩小。
  - 计算器输入项垂直排列。

---

## 11. 边界与错误处理

| 场景 | 处理 |
|---|---|
| 未登录用户访问 `/guide` | 正常展示，涉及登录后才能操作的功能按钮引导去登录。 |
| 排行榜数据未加载/不可用 | 预览卡片显示固定 mock 数据，不报错、不空白。 |
| 计算器输入非法值 | 结果区提示“请输入有效数字”，不崩溃。 |
| URL 锚点不存在 | 默认滚动到第一个分类，不报错。 |
| 移动端目录抽屉打开后点击正文区 | 自动关闭抽屉，提升阅读体验。 |

---

## 12. 测试计划

1. 首页顶部导航出现「玩法指南」，点击进入 `/guide`。
2. `/guide` 页面左侧目录 4 个分类可点击，右侧正确滚动到对应锚点。
3. 时间节省计算器输入不同数值，输出结果正确。
4. 收益排行榜预览卡片展示 TOP 5，点击查看完整榜单跳转正确（已登录/未登录分别验证）。
5. 首页 Hero 区小字链接可点击进入 `/guide`。
6. 进入控制台后若未生成过文章，顶部出现新手横幅，点击可进入 `/guide`；生成一篇文章后横幅消失。
7. 「我的账户」空收益状态出现“看看怎么赚创作币”链接。
8. 暗色主题下指南页、目录、计算器、排行榜卡片样式正确。
9. 移动端目录抽屉可正常打开/关闭，正文无横向溢出。

---

## 13. 实现位置汇总

| 文件 | 操作 |
|---|---|
| `project/user/web/src/views/GuideIndex.vue` | 新建：玩法指南页面容器 |
| `project/user/web/src/components/guide/GuideSidebar.vue` | 新建：左侧目录组件 |
| `project/user/web/src/components/guide/GuideArticle.vue` | 新建：右侧文章渲染组件 |
| `project/user/web/src/components/guide/TimeCalculator.vue` | 新建：时间节省计算器 |
| `project/user/web/src/components/guide/LeaderboardPreview.vue` | 新建：排行榜预览卡片 |
| `project/user/web/src/data/guide-content.js` | 新建：指南文案内容数据 |
| `project/user/web/src/router/index.js` | 修改：新增 `/guide` 路由 |
| `project/user/web/src/views/Home.vue` | 修改：导航加入口，Hero 区加辅助链接 |
| `project/user/web/src/views/console/ConsoleLayout.vue` | 修改：新增新手引导横幅 |
| `project/user/web/src/views/console/EarningsIndex.vue` | 修改：空收益状态加玩法指南链接 |
| `project/user/web/src/views/console/LeaderboardIndex.vue` | 修改：规则弹框加完整指南链接 |
| `project/user/web/src/composables/useLeaderboard.js` | 读取：排行榜预览数据（若已实现） |
