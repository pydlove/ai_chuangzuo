# 风格市场视觉升级设计

**日期**: 2026-07-27
**状态**: 已确认，待实现
**关联**: `docs/superpowers/specs/2026-07-02-style-market-design.md`（v1 功能规格）；`docs/design/design-system.md`（视觉规范）
**作用范围**: 仅展示层重做，**不改**任何数据模型、计费规则、状态机、路由

---

## 1. 目标

把 `StyleMarketIndex.vue` 从「功能列表页」升级为「平台门面页」：

- **高大上** — 平台级视觉密度，更细致的卡片层级、统一规范的圆角/字号/阴影
- **官方** — 平台认证标识、官方精选区、平台门面 banner
- **可收益** — 顶部实时收益数据、上传激励卡、创作者收益潜力榜

本次重做**不影响**计费（消费者使用不扣币、创作者 +0.2 币/次，周里程碑），不影响任何现有交互路径。

---

## 2. 设计决策

| 维度 | 决策 |
|---|---|
| 实现位置 | 只动 `project/user/web/src/views/console/StyleMarketIndex.vue` 与 `project/user/web/src/composables/useStyleMarket.js` |
| 路由 | `/console/style-market` 不变 |
| 色板 | 主色沿用 `--color-primary` 红 `#ff2442`；**不引入金色**（设计系统禁用非主色作为强彩色；用「徽章 + 字号 + 留白 + 加深阴影」表达官方感） |
| 圆角 | 全部 ≤12px（卡片用 `--radius-xl` 12；按钮/输入 `--radius-lg` 8；徽章 `--radius-md` 6） |
| 字号 | 严格使用 `--font-h1 / h2 / h3 / body / small / caption` 体系，**不出现 17px / 13px 之类非规范字号** |
| 阴影 | 卡片静态 `--shadow-sm2`；hover `--shadow-md` + `translateY(-6px)` |
| 暗色主题 | 所有新加类提供 `body[data-theme="dark"]` 适配 |
| 响应式 | 沿用项目 `768px` 断点 |
| 计费口径 | **以代码为准**：`useMarketStyle` 是消费者不扣币、创作者 +0.2 币（v1 spec 写的"扣消费者 0.2"与代码不符；本文档不再纠结，按代码走） |

---

## 3. 页面信息架构

页面自上而下 5 个区块（每区块独立 SPA-visible 锚点 / class）：

```
┌────────────────────────────────────────────────────────────────┐
│  ① 平台 Banner 区  — 门面 + 实时数据                           │
├────────────────────────────────────────────────────────────────┤
│  ② 上传激励卡      — 强化"上传也能赚"                          │
├────────────────────────────────────────────────────────────────┤
│  ③ 官方精选大卡    — 横滑 4-5 张「官方精选」                    │
├────────────────────────────────────────────────────────────────┤
│  ④ 收益潜力榜      — Top 5 创作者横向条                        │
├────────────────────────────────────────────────────────────────┤
│  ⑤ 全部风格区      — tab + 卡片网格（升级视觉）                │
└────────────────────────────────────────────────────────────────┘
```

区块 ① ⑤ 是旧有（升级版），② ③ ④ 是新增。

---

## 4. 各区块规范

### 4.1 ① 平台 Banner

- **背景**：白色 `--color-bg-card`，圆角 `--radius-xl` 12，padding 32 24，阴影 `--shadow-sm2`。
- **左侧**：
  - H1「爱创作 · 风格市场」`(font-h1, color-primary, font-weight: 700)`
  - 副文案：`官方运营 · 精选创作者风格 · 使用即获收益分成`（`font-body`, `text-secondary`）
  - 「收益规则」主色文字链（`font-body`, `color-primary`，hover `color-primary-hover`，下划线 2px `text-underline-offset: 4px`）
- **右侧**：三块数据芯片（chip），每块结构：
  - 数字 H2 `(20px / 700 / text-primary)`
  - 标签 `font-small / text-secondary`
  - 例：`已上架 28 款` / `累计使用 4,216 次` / `累计发放 843.2 币`
  - 芯片背景 `--color-bg-page`，圆角 `--radius-xl` 12，padding 12 20，gap 32 横向排布。
- **响应式**：≤768px 时整块改为单列（标题居上，数据芯片置下且横滑）。

### 4.2 ② 上传激励卡

- 整卡 `padding: 20 24`，背景 `--color-bg-card`，圆角 `--radius-xl` 12。
- 左边一条 `4px` 宽 `--color-primary` 色条（`border-left: 4px solid var(--color-primary)`）。
- **左中右三栏** flex：
  - 左：图标（24×24，emoji 或 SVG）；文案 `上传你的风格，开始赚创作币`(`font-h3 / 700 / text-primary`)
  - 中：副文案 `每被他人使用 1 次即得 0.2 币；周里程碑最高额外 +60`(`font-small / text-secondary`)
  - 右：主按钮 `立即上架`(`height: 40px`, 圆角 `radius-lg` 8, 背景主色白字；hover `color-primary-hover`)
- 整卡可点击，路由 `/console/styles`（我的风格页）—— 文案：v1 已存在此链接，本卡片复用之，不新增 anchor。

### 4.3 ③ 官方精选大卡

- 区块标题：`H2 官方精选`（`font-h2 / 700 / text-primary`）+ 右侧徽章 `官方`（背景 `color-primary`、白字、`font-caption` 11、圆角 `radius-md` 6、padding 2 8）。
- 右侧文末：「查看全部 →」次按钮（见底部规则）。
- **横滑 4-5 张大卡**，每张高 200，宽 360，间距 16：
  - 深色背景：`linear-gradient(135deg, #1a1a1a 0%, #2a1015 100%)`（深红渐变）
  - 顶部 padding 24 24，下 padding 24 24，左对齐文案
  - 风格名 `font-h3 / 700 / #fff`，最多两行（`-webkit-line-clamp: 2`）
  - 适用范围 tag：底部左下角，浮于深底之上（白字、红底胶囊）
  - 关键数据 `🔥 本周使用 142 次`(`font-h2 / 700 / #fff`)，紧贴底部
  - 角标 `by 林晚 · 累计赚 156 币`(`font-caption / rgba(255,255,255,0.7)`)，绝对定位底部右下
- 横滑使用原生 `overflow-x: auto + scroll-snap-type: x mandatory`，移动端 touch 友好。
- 取数：`featuredStyles` —— `marketStyles` 中 `status === 'approved' && totalUses >= 5`，按 `totalUses desc`，取前 8 做池，横滑展示前 5（移动端展示前 3）。

### 4.4 ④ 收益潜力榜

- 区块标题 `H2 收益潜力榜`(可附副标 `看看谁在用风格赚到币` `font-body / text-secondary`)。
- **横向 Top 5 创作者小卡**：
  - 卡片宽 200，高 200，圆角 `radius-xl` 12，背景 `--color-bg-card`，阴影 `shadow-sm2`，padding 16。
  - 顶部：圆形 56 头像（背景 `--color-primary`、姓名首字白、`font-h3`）
  - 中部：姓名 `font-body / 700 / text-primary`
  - 「本周 +42 币」`font-h2 / 700 / color-primary`
  - 副标 `代表风格 · 名` `font-small / text-secondary`
  - 「使用」主按钮，置底，宽度 100%
- 取数：`topCreators` —— 按 creatorId 聚合 `marketStyles` 的 `weeklyEarnings`，降序，前 5 名。
- 排版：横向 flex 横向滑动 + 右侧「完整榜单 →」次按钮。

### 4.5 ⑤ 全部风格区

- 区块标题 `H2 全部风格`，右侧 `H2 font-body text-secondary` 显示 `共 X 款`。
- **tab 改为胶囊式分段控件**（受 `2026-07-26-commission-center-visual-design.md` 启发）：
  - 单行容器 `padding: 4`，背景 `--color-bg-page`，圆角 `radius-lg` 8。
  - 单个 tab `padding: 8 16`，无边框，激活态 `bg-card + shadow-sm + text-primary`，未激活 `text-secondary`。
  - 5 个 tab：`全部 / 本周最热 / 历史最热 / 最新 / 官方精选`。
- **卡片升级**：
  - 网格：`grid-template-columns: repeat(auto-fill, minmax(320px, 1fr))`，gap 24，移动端单列。
  - 卡片宽 `320-400`、padding 20，圆角 `radius-xl` 12，背景 `--color-bg-card`，静态 `shadow-sm2`，hover `shadow-md + translateY(-6px)`。
  - **顶部封面区 80px 高**：深色渐变 `#1a1a1a → #2a1015`，适用范围 tag 浮在封面左上（红底白字 12 caption 圆角 `radius-md` 6）。
  - **主体（封面下方）**：
    - 风格名 `font-h3 / 700 / text-primary`，单行省略
    - 「by 创作者头像(24×24 圆形)+ 名字」`font-body / text-regular`
    - 「我的」蓝标 `info` 色，仅 `creatorId === currentUserId` 时显示
    - prompt 摘要截 60 字 + 「展开」按钮，限 4 行 `(-webkit-line-clamp: 4)`
    - 数据条 `本周 X 次 · 累计 X 次` `font-small / text-placeholder`，前置 🔥 图标（emoji 或 SVG）
  - **底部 actions**：
    - 「使用」主按钮（40 高、`radius-lg`、白字红底），左对齐
    - `♡ 收藏 + 查看`，次按钮（36 高、`radius-lg`、border-default）
    - 「模拟」按钮只对 `s.creatorId === currentUserId` 显示（v1 已实现，本期保留）

---

## 5. 数据层（聚合）

在 `useStyleMarket.js` 新增 3 个 computed（**只聚合，不写 storage**）。

**数据来源原则**：`weeklyUses / totalUses / weeklyEarnings` 取自每条 `marketStyles` 单记录；`totalEarnings`（创作者总收益）和 `marketStats.totalEarnings` 取自 `earningsRecords`，跨风格按 `styleId → creatorId` 关联。

```js
// 0. 预聚合：创作者总收益（来自 earningsRecords）
const totalEarningsByCreator = computed(() => {
  const map = {}
  earningsRecords.value.forEach((r) => {
    if (!(r.amount > 0)) return
    const s = marketStyles.value.find((m) => m.id === r.styleId)
    if (!s) return
    map[s.creatorId] = (map[s.creatorId] || 0) + r.amount
  })
  return map
})

// ① 平台统计
export const marketStats = computed(() => {
  const approved = marketStyles.value.filter((s) => s.status === 'approved')
  return {
    approvedCount: approved.length,
    totalUses: approved.reduce((sum, s) => sum + (s.totalUses || 0), 0),
    totalEarnings: Object.values(totalEarningsByCreator.value)
      .reduce((sum, v) => sum + v, 0)
  }
})

// ② Top 5 创作者（按 weeklyEarnings 降序；总收益用 earningsRecords 实时聚合）
export const topCreators = computed(() => {
  const map = new Map()
  marketStyles.value
    .filter((s) => s.status === 'approved')
    .forEach((s) => {
      const cur = map.get(s.creatorId) || {
        creatorId: s.creatorId,
        creatorName: s.creatorName || '匿名用户',
        weeklyEarnings: 0,
        weeklyUses: 0,
        bestStyle: null
      }
      cur.weeklyEarnings += s.weeklyEarnings || 0
      cur.weeklyUses += s.weeklyUses || 0
      if (!cur.bestStyle || (s.totalUses || 0) > (cur.bestStyle.totalUses || 0)) {
        cur.bestStyle = s
      }
      map.set(s.creatorId, cur)
    })
  const tbm = totalEarningsByCreator.value
  return Array.from(map.values())
    .map((c) => ({ ...c, totalEarnings: tbm[c.creatorId] || 0 }))
    .sort((a, b) => b.weeklyEarnings - a.weeklyEarnings)
    .slice(0, 5)
})

// ③ 官方精选池（approved 且 totalUses >= 5，按 totalUses 降序，取 8）
export const featuredStyles = computed(() =>
  marketStyles.value
    .filter((s) => s.status === 'approved' && (s.totalUses || 0) >= 5)
    .sort((a, b) => (b.totalUses || 0) - (a.totalUses || 0))
    .slice(0, 8)
)
```

附加约束：

- `currentUserId` 取 `localStorage.getItem('aichuangzuo_user_id')`，缺失则视为已登录 mock 用户（与 v1 一致）。
- 不动 `getCoinBalance` / `useMarketStyle` / `settleWeeklyMilestone` 等函数。
- 不动 localStorage 键名 / 数据结构。

---

## 6. 文案与口径

| 位置 | 文案 | 备注 |
|---|---|---|
| ① 标题 | 爱创作 · 风格市场 | 与 ConsoleLayout 路由标题区分（路由用「风格市场」） |
| ① 副 | 官方运营 · 精选创作者风格 · 使用即获收益分成 | 「官方运营」是官方感主要文字锚点 |
| ① chipA | 已上架 N 款 | 显示 `marketStats.approvedCount` |
| ① chipB | 累计使用 N 次 | 显示 `marketStats.totalUses` |
| ① chipC | 累计发放 N 创作币 | 显示 `marketStats.totalEarnings`，2 位小数 |
| ② 标题 | 上传你的风格，开始赚创作币 | |
| ② 副 | 每被他人使用 1 次即得 0.2 币；周里程碑最高额外 +60 | |
| ② CTA | 立即上架 | 跳 `/console/styles` |
| ③ 区标 | 官方精选 | |
| ③ 徽章 | 官方 | |
| ④ 区标 | 收益潜力榜 | |
| ④ 副 | 看看谁在用风格赚到币 | |
| ⑤ 区标 | 全部风格 | 右侧 `共 N 款` |
| ⑤ tab | 全部 / 本周最热 / 历史最热 / 最新 / 官方精选 | |

---

## 7. 边界与错误处理

| 场景 | 处理 |
|---|---|
| 没有任何 `approved` 风格 | 区块 ③ 显示空态卡片「官方精选即将上线」(`text-placeholder`, 高度 200) |
| `topCreators` 为空 | 区块 ④ 显示「暂无上榜创作者」 |
| `featuredStyles` 为空 | 同 ③ |
| 当前用户已上架但未获收益 | 头像 / 名字 / 收益区正常显示，0 显示为 `0.0` |
| `localStorage` 无 userId | 视为已登录 mock，与 v1 行为一致 |
| 暗色主题 | 全部新增区域需 `body[data-theme="dark"]` 适配 —— 已在第 8 节列出覆盖清单 |
| 移动端 | Banner chip 横滑、外层卡片 padding 改为 16、横滑卡片宽 320 卡间距 12 |

---

## 8. 暗色主题适配清单

新增类必须在暗色下覆盖：

- `.market-banner` 背景 → `#1f1f1f`；左侧 H1 / chip 内数字 / 副文案按现有 `--text-primary / text-secondary` 语义对应深色变量
- `.market-upload-card` 背景 `#1f1f1f`，左边红条不变
- `.market-featured-card` 深色底本来就深，不变
- `.market-creator-card` 背景 `#1f1f1f`，文字按深色语义变量
- `.market-style-card` 背景 `#1f1f1f`，封面保留暗渐变（视觉一致）
- 现有 `.style-market-*` 类由原 scss 已写暗色适配，本期**新增类不复用旧类名**，避免耦合

---

## 9. 测试要点（手动 E2E 验证）

1. **页面 5 个区块渲染** — 故意清空 localStorage 后再访问页面（确保空态可读）。
2. **数据正确性** — 在 EarningsIndex 走一遍使用流程，回到 MarketIndex，Banner 数据自动刷新。
3. **横滑体验** — 电脑端滚轮 + 移动端 touch，左右横滑 5 张无卡顿；外层不被页面滚动错误接管。
4. **响应式** — `768px` 断点单列、chip 横滑、卡片单列；右侧 chip 不挤出。
5. **暗色主题** — 切换主题后所有区块无白底闪烁、无对比度问题。
6. **可访问性** — 主色对比 AA；按钮 focus ring 保留；卡片 tab 可键盘 focus。
7. **现有功能不回归** — 点击「使用」依然跳 `/console/create?marketStyleId=xxx`；「♡ 收藏」行为不变；「模拟」按钮仅自己可见且行为不变；「收益规则」弹框打开依旧。
8. **垃圾代码审计** — 老的 `.style-market-*` 类如被新模板根类完全替代则删除；`unused selector` 全清（按 CLAUDE.md 清理原则）。

---

## 10. 实现位置汇总

| 文件 | 改动 |
|---|---|
| `project/user/web/src/views/console/StyleMarketIndex.vue` | 重写 template + style scoped，5 区块分层结构 |
| `project/user/web/src/composables/useStyleMarket.js` | 新增 3 个 computed：`marketStats` / `topCreators` / `featuredStyles` |

**不动**：路由 `router/index.js`、`EarningsIndex.vue`、`StylesIndex.vue`、`CreateIndex.vue`、`ConsoleLayout.vue`、所有 `*Index.vue`、`useStyles.js`、`api/*`、后端、mock 种子数据。

---

## 11. 非目标

- 不做「申请官方认证」流程（精选由 `totalUses ≥ 5` 触发的逻辑足够）
- 不做创作者详情抽屉
- 不改计费、状态机、路由
- 不接后端，纯前端聚合
- 不引入新色板、不新增图片资源

---

## 12. 风险与权衡

- **空态**：理论上 `marketStyles` 可能为空；已对三个区块分别给空态文案，避免白屏。
- **`feature/user-style-analyze` 分支**：当前 branch 上有未提交的 `M` 状态（项目代码无关文件）；本次只提交 spec 文件，不污染既有改动。
- **设计系统合规**：金色辅助色已主动放弃，避免后续被 `docs/design/design-system.md` 维护者打回；如未来真要金色，先升 design-system。
