# 收益排行榜设计

**日期**: 2026-07-02
**状态**: 已确认，待实现
**关联文件**: `project/user/web/src/views/console/LeaderboardIndex.vue`、`project/user/web/src/composables/useLeaderboard.js`、`project/user/web/src/views/console/ConsoleLayout.vue`、`project/user/web/src/router/index.js`、`project/user/web/src/composables/useStyleMarket.js`

---

## 1. 功能概述

在 Vue 控制台新增「收益排行榜」独立页面，包含两块榜单：

1. **创作币榜**：按自然月统计平台用户在爱创作获得的创作币收益，展示 TOP 排名；月度前 10 名各奖励 100 创作币。
2. **自媒体收入榜**：支持查看月度榜与年度榜；用户可申报自己在公众号、小红书、抖音等自媒体平台的真实收入，填写金额并上传收益截图，前端模拟审核通过后金额累加生效；月度前 10 名各奖励 100 创作币。

页面同时提供完整的「榜单规则」说明弹框。

**范围**：仅在 Vue 控制台项目（`project/user/web/`）内实现，前端 localStorage mock，不接入后端。

---

## 2. 设计决策

- **实现位置**：新建 `LeaderboardIndex.vue`、`useLeaderboard.js`，修改 `ConsoleLayout.vue`（侧边栏新增菜单）、`router/index.js`（新增路由）。
- **页面组织**：采用「单页 + Tab 切换」方案。页面内顶部两个 Tab 切换「创作币榜」「自媒体收入榜」，避免新增多个菜单项。
- **创作币榜数据源**：复用现有 `aichuangzuo_earnings_records` 创作币流水，按自然月聚合当前用户真实收益；榜单上其他 19 个用户为前端 mock 用户，金额按「月份 + 用户ID」做确定性随机生成。
- **自媒体收入榜数据源**：新建 `aichuangzuo_leaderboard_income_submissions` 存储用户按月申报的收入记录；月度榜直接聚合该月 approved 金额，年度榜汇总该自然年内所有 approved 月度金额。用户仅需按月申报，无需单独申报年度收入。
- **审核流程**：和「风格市场」一致，前端提供「模拟通过 / 模拟拒绝」按钮用于演示。
- **奖励发放**：每月 1 日自动结算上月榜单；页面同时提供「模拟发放上月奖励」按钮便于即时演示。
- **规则说明**：复用现有规则弹框样式（`a-modal`，宽度 560px，居中，无 footer）。

---

## 3. 数据模型

### 3.1 创作币榜单（派生数据，不新增独立 key）

基于现有 `aichuangzuo_earnings_records`（由 `useStyleMarket.js` 维护）按月份聚合。

聚合逻辑：

```js
// 以 createdAt 所在自然月为维度，按 fromUserId 聚合 amount > 0 的收益
{
  userId: 'u_xxx',
  nickname: '我',
  avatar: '',
  month: '2026-07',
  amount: 12.4  // 创作币
}
```

### 3.2 自媒体收入申报

用户只需按月申报，年度榜由月度数据自动汇总。localStorage key：`aichuangzuo_leaderboard_income_submissions`

```js
[
  {
    id: 'income-xxx',
    userId: 'u_xxx',
    month: '2026-07',             // 申报所属月份，格式 'YYYY-MM'
    amount: 3580.50,              // 元
    screenshot: 'data:image/png;base64,...',
    status: 'pending',            // pending | approved | rejected
    createdAt: '2026-07-02T...',
    auditedAt: '',
    rejectReason: ''
  }
]
```

### 3.3 榜单奖励发放记录

localStorage key：`aichuangzuo_leaderboard_rewards`

```js
[
  {
    periodType: 'month',
    periodValue: '2026-06',
    leaderboardType: 'coin',      // 'coin' | 'income'
    rank: 3,
    userId: 'u_xxx',
    amount: 100,
    awardedAt: '2026-07-01T...'
  }
]
```

---

## 4. 架构与组件

### 4.1 useLeaderboard.js（新建）

核心状态与函数：

```js
import { ref, computed } from 'vue'

export const incomeSubmissions = ref(loadIncomeSubmissions())
export const rewardRecords = ref(loadRewardRecords())

// 生成创作币榜（当前用户真实数据 + mock 用户）
export function getCoinLeaderboard(month) { }

// 生成自媒体收入榜（当前用户 approved 月度数据 + mock 用户；年度榜由月度汇总）
export function getIncomeLeaderboard(periodType, periodValue) { }

// 提交自媒体收入申报
export function submitIncomeSubmission(submission) { }

// 模拟审核通过
export function approveIncomeSubmission(id) { }

// 模拟审核拒绝
export function rejectIncomeSubmission(id, reason) { }

// 检查并发放上月榜单奖励
export function maybeAwardMonthlyRewards() { }

// 模拟发放指定月份奖励（演示用）
export function simulateAwardMonthlyRewards(periodValue) { }
```

### 4.2 LeaderboardIndex.vue（新建）

- 页面标题区：标题、副标题、规则按钮。
- Tab 切换：「创作币榜」「自媒体收入榜」。
- 周期选择器：
  - 创作币榜：月份选择。
  - 自媒体收入榜：「月度 / 年度」切换 + 月份/年份选择。
- 榜单主体：前 3 名突出卡片 + 4-20 名列表行。
- 当前用户高亮：上榜行左侧显示「我」标签。
- 操作区：
  - 创作币榜：无额外操作。
  - 自媒体收入榜：「申报收入」按钮、「模拟发放上月奖励」按钮。
- 我的申报记录：仅自媒体收入榜展示，显示当前用户所有申报及审核按钮。
- 规则弹框：`a-modal` 展示榜单规则。
- 申报弹框：`a-modal` 含周期、金额、截图上传。

### 4.3 ConsoleLayout.vue 改动

在 `navItems` 同作用域内引入 `TrophyOutlined` 图标，并在 `navItems` 中新增：

```js
{ path: '/console/leaderboard', label: '收益排行榜', icon: TrophyOutlined }
```

### 4.4 router/index.js 改动

新增子路由：

```js
{
  path: 'leaderboard',
  name: 'ConsoleLeaderboard',
  component: () => import('@/views/console/LeaderboardIndex.vue')
}
```

---

## 5. 页面结构与交互

```
┌─────────────────────────────────────────────┐
│ 收益排行榜                    [榜单规则]      │
│ 查看创作激励排名，冲击月度榜单奖励             │
├─────────────────────────────────────────────┤
│ [创作币榜] [自媒体收入榜]                     │
├─────────────────────────────────────────────┤
│ 周期：[2026-07 ▾]              [申报收入]    │
├─────────────────────────────────────────────┤
│ 🥇 用户 A              12,580 元   已获奖励  │
│ 🥈 用户 B               9,200 元   已获奖励  │
│ 🥉 用户 C               7,150 元   待结算    │
│  4 用户 D               5,000 元            │
│ ...                                        │
│  8 我                   1,200 元            │
├─────────────────────────────────────────────┤
│ 我的申报记录                                │
│ 2026-07  ·  3,000 元  · 审核中  [通过][拒绝] │
└─────────────────────────────────────────────┘
```

---

## 6. 榜单排行逻辑

### 6.1 创作币榜

- 仅支持按自然月查看，默认当前月。
- 从 `earningsRecords` 过滤 `createdAt` 落在该月且 `amount > 0` 的记录，按 `fromUserId` 聚合。
- 当前用户真实金额 + 19 个 mock 用户组成榜单，按金额倒序排列。
- mock 用户金额使用 `hash(month + userId)` 做确定性随机，保证同月刷新不变、换月变化。
- 前 3 名使用金/银/铜色背景与冠亚季军徽章，当前用户行高亮并加「我」标签。

### 6.2 自媒体收入榜

- 支持「月度 / 年度」切换，默认月度。
- 月度榜：聚合该月所有 `status === 'approved'` 的申报金额。
- 年度榜：汇总该自然年内所有 approved 月度申报金额。
- 榜单成员 = 当前用户 + 其他有 approved 申报的 mock 用户；若人数不足 20，按实际人数展示。
- 排序与展示样式与创作币榜一致。

### 6.3 空状态

- 创作币榜当前用户无收益：显示「暂无数据，快去创作赚币吧」，并提供跳转「风格市场」「我的账户」的链接。
- 自媒体收入榜无人申报：显示「暂无申报记录，点击右上角申报收入」。

---

## 7. 自媒体收入申报与审核流程

### 7.1 申报入口

- 「自媒体收入榜」页面右上角常驻「申报收入」按钮。
- 点击打开申报弹框（`a-modal`，宽度 480px）。

### 7.2 申报表单

- 申报月份：月份选择（如 2026-07）。
- 收入金额：数字输入，单位「元」，最小 1，保留两位小数。
- 收益截图：文件选择，仅 jpg/png，最多 1 张，前端转 Base64 预览。
- 提交按钮：校验通过后可点击。

> 说明：自媒体收入只需按月申报，年度榜单由月度 approved 记录自动汇总生成。

### 7.3 校验规则

- 金额必须大于 0。
- 必须上传截图。
- 截图文件大小不超过 5MB，格式错误时提示「请上传 jpg/png 格式的图片」。
- 同周期已有审核中申报时提示「你已有该周期的申报在审核中，可继续提交补充材料」。

### 7.4 审核状态

- 提交后 `status = 'pending'`，金额暂不计入榜单。
- 「我的申报记录」区域展示当前用户所有申报：周期、金额、缩略图、状态、审核时间。
- 为演示审核，每条 pending 记录旁显示：
  - 「模拟通过」：金额累加进榜单，`status = 'approved'`，`auditedAt` 写入当前时间。
  - 「模拟拒绝」：金额不计入，`status = 'rejected'`，可填写拒绝原因。

### 7.5 累加规则

- 同一周期可多次申报，approved 金额累加生效。
- rejected 金额不影响已通过金额。

---

## 8. 奖励发放规则

### 8.1 奖励对象

- 创作币榜：自然月结束时，排名前 10 的用户各获得 100 创作币。
- 自媒体收入榜：自然月结束时，月度排名前 10 的用户各获得 100 创作币。年度榜仅展示，不设奖励。

### 8.2 发放时机

- 每月 1 日 00:00 自动结算上月榜单并发放奖励。
- 页面打开时检测：若当前时间已进入新的自然月，且上月奖励未发放过，则自动给上榜用户发放 100 创作币，写入 `aichuangzuo_leaderboard_rewards`，并写入 `aichuangzuo_earnings_records` 类型为 `leaderboard_reward` 的记录。

### 8.3 展示方式

- 历史月份榜单：TOP 10 行右侧显示「已获 100 创作币」绿色标签。
- 当前月份榜单：TOP 10 行右侧显示「本月榜单进行中，待结算」灰色标签。
- 获奖记录可在「我的账户」收益明细中查看。

### 8.4 演示控制

- 页面提供「模拟发放上月奖励」按钮（仅前端 mock）。
- 点击后立即为上月上榜用户发放奖励并更新创作币余额，便于非 1 号日期也能验证奖励效果。

---

## 9. 榜单规则说明

### 9.1 入口

- 页面标题区右侧放置「榜单规则」文字按钮，点击打开规则弹框。
- 弹框样式与「我的账户」「风格市场」的规则弹框一致（`a-modal`，宽度 560px，居中，无 footer）。

### 9.2 规则内容

1. **创作币榜**：按自然月统计用户在平台获得的创作币收益（含风格市场收益、邀请返利等），排名前 10 的用户次月 1 日各获得 100 创作币奖励。
2. **自媒体收入榜**：支持查看月度榜与年度榜；用户可申报自己在自媒体平台（公众号、小红书、抖音等）的真实收入，填写金额并上传收益截图，审核通过后金额累加生效。
3. **月度奖励**：自媒体收入榜每月排名前 10 的用户，次月 1 日各获得 100 创作币奖励；年度榜仅做展示，不设奖励。
4. **审核说明**：提交后进入审核，审核中金额不计入榜单；如截图不清晰、金额存疑或涉嫌造假，平台有权拒绝申报。
5. **公平性**：禁止通过虚假截图、重复申报、机器刷量等违规方式获取排名，一经发现取消当月排名及奖励。
6. 底部统一加「* 活动最终解释权归平台所有」。

---

## 10. 边界与错误处理

| 场景 | 处理 |
|---|---|
| 未登录/无用户ID | 控制台页面默认已登录；若取不到 `userId` 则自动生成一个并持久化。 |
| 当前月无收益 | 创作币榜当前用户显示 0，排名垫底，提供引导跳转。 |
| 榜单人数不足 20 | 按实际人数展示，不补空行。 |
| 重复申报 | 同一周期多次申报金额可累加；多次拒绝不影响已通过金额。 |
| 截图过大/格式错误 | 提示「请上传不超过 5MB 的 jpg/png 图片」。 |
| 切换周期 | 榜单重新计算，mock 用户金额基于周期种子保持一致。 |
| 已发奖月份 | 再次打开不再重复发放，以 `leaderboardType + periodValue + userId` 去重。 |
| 当前月份 | 不发放奖励，仅展示「待结算」。 |
| 无申报记录 | 自媒体收入榜显示空状态并引导申报。 |

---

## 11. 测试计划

1. 侧边栏出现「收益排行榜」，点击进入 `/console/leaderboard`。
2. 创作币榜默认展示当前月，当前用户行高亮，前 3 名有冠亚季军样式。
3. 切换月份，排名按新周期重新计算且不重复。
4. 切换到自媒体收入榜，点击「申报收入」弹框，提交金额和截图。
5. 申报记录显示「审核中」，点击「模拟通过」后金额累加进榜单排名。
6. 月度榜前 10 名出现「待结算」标签；点击「模拟发放上月奖励」后变为「已获 100 创作币」，余额增加。
7. 规则弹框内容完整，关闭正常。
8. 暗色主题下榜单、弹框、申报记录样式正确。
9. 移动端适配：Tab、选择器、榜单行不溢出。

---

## 12. 实现位置汇总

| 文件 | 操作 |
|---|---|
| `project/user/web/src/composables/useLeaderboard.js` | 新建：榜单数据生成、申报、审核、奖励发放 |
| `project/user/web/src/views/console/LeaderboardIndex.vue` | 新建：收益排行榜页面 |
| `project/user/web/src/views/console/ConsoleLayout.vue` | 修改：侧边栏新增「收益排行榜」菜单，引入 `TrophyOutlined` |
| `project/user/web/src/router/index.js` | 修改：新增 `/console/leaderboard` 路由 |
| `project/user/web/src/composables/useStyleMarket.js` | 可选修改：奖励发放时写入 `earningsRecords`，新增 `leaderboard_reward` 类型 |
| `project/user/web/src/views/console/EarningsIndex.vue` | 可选修改：`formatType` 增加 `leaderboard_reward` 显示为「榜单奖励」|
