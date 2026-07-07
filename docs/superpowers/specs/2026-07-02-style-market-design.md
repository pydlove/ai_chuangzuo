# 风格市场设计

**日期**: 2026-07-02
**状态**: 已确认，待实现
**关联文件**: `project/user/web/src/views/console/StyleMarketIndex.vue`、`project/user/web/src/views/console/EarningsIndex.vue`、`project/user/web/src/composables/useStyleMarket.js`、`project/user/web/src/composables/useStyles.js`、`project/user/web/src/views/console/ConsoleLayout.vue`、`project/user/web/src/views/console/StylesIndex.vue`、`project/user/web/src/router/index.js`、`project/user/web/src/views/console/CreateIndex.vue`

---

## 1. 功能概述

新增「风格市场」模块，用户可将自己的「我的风格」或「学习的风格」分享到市场；经管理员审核通过（本期前端 mock，管理端暂不实现）后上架，其他用户可在市场发现并使用。每次使用扣 0.2 创作币，创作者实时获得 0.2 创作币；每周按使用次数发放里程碑奖励。

**范围**：仅在 Vue 控制台项目（`project/user/web/`）内实现，前端 localStorage mock，不接入后端。

## 2. 设计决策

- **实现位置**：新建 `StyleMarketIndex.vue`、`EarningsIndex.vue`、`useStyleMarket.js`，修改现有 `useStyles.js`、`StylesIndex.vue`、`ConsoleLayout.vue`、`router/index.js`、`CreateIndex.vue`。
- **分享对象**：「我的风格」与「学习的风格」均可分享，系统预设不可分享。
- **审核流程**：分享后状态为 `pending`，管理端审核通过为 `approved`；本期前端提供「模拟通过」按钮用于演示。
- **入口**：`ConsoleLayout.vue` 侧边栏「我的风格」下新增子菜单：「我的风格」「风格市场」「收益明细」。
- **计费**：每次使用扣 0.2 创作币，创作者实时 +0.2 创作币。
- **里程碑奖励**：按自然周统计使用次数，周日 23:59 自动结算，只取最高档发放一次。
- **余额精度**：支持小数，localStorage 存字符串避免浮点问题。
- **创作页联动**：市场页点击「使用」跳转 `/console/create?marketStyleId=xxx`，创作页读取参数后自动应用风格。

## 3. 数据模型

```js
// 市场上架列表
// localStorage key: aichuangzuo_style_market
[
  {
    id: 'market-xxx',
    name: '娱乐至死',
    sourceType: 'learned',        // 'my' | 'learned'
    originalName: '娱乐至死',      // 原风格名
    creatorId: 'user-xxx',         // 创作者标识（复用现有 userId）
    creatorName: '我',             // 展示用，mock 固定为「我」或生成昵称
    prompt: '...',
    scope: '公众号情感文',
    excerpt1: '...',               // 学习的风格保留，我的风格可空
    excerpt2: '...',
    status: 'approved',            // pending | approved | rejected
    price: 0.2,                    // 单次使用价格（创作币）
    weeklyUses: 12,                // 本周使用次数
    totalUses: 156,                // 累计使用次数
    weeklyEarnings: 2.4,           // 本周基础收益（price * weeklyUses）
    milestoneBonus: 0,             // 本周已发放里程碑奖励
    lastSettlementAt: '2026-07-01T00:00:00Z', // 上次结算时间
    createdAt: '2026-07-02T...'
  }
]

// 收益明细
// localStorage key: aichuangzuo_earnings_records
[
  {
    id: 'earn-xxx',
    type: 'usage',                 // usage | milestone | settlement
    styleName: '娱乐至死',
    styleId: 'market-xxx',
    amount: 0.2,                   // 创作币
    fromUserId: 'user-yyy',        // 使用者，usage 时有
    description: '用户使用风格生成文章',
    createdAt: '2026-07-02T...'
  }
]

// 用户创作币余额（复用现有 key）
// localStorage key: aichuangzuo_coin_balance
'25.6'
```

## 4. 架构与组件

### useStyleMarket.js（新建）

核心状态与函数：

```js
import { ref } from 'vue'

export const marketStyles = ref(loadMarketStyles())
export const earningsRecords = ref(loadEarningsRecords())

// 分享风格到市场
export function shareStyleToMarket(style, sourceType) {
  // 复制风格数据，status = pending
}

// 模拟审核通过（演示用）
export function approveMarketStyle(marketId) {
  // 找到对应 style，status = approved
}

// 使用市场风格
export function useMarketStyle(marketId, consumerUserId) {
  // 检查余额 >= 0.2
  // 扣消费者 0.2，给创作者 +0.2
  // marketStyle.weeklyUses++ / totalUses++
  // 写入 earningsRecords
}

// 每周结算
export function settleWeeklyMilestone() {
  // 检查是否需要结算
  // 按 weeklyUses 取最高档发放 milestoneBonus
  // 写入 earningsRecords，重置 weeklyUses
}

// 模拟其他用户使用（单用户演示）
export function simulateExternalUse(marketId) {
  // 给某个风格 +1 使用并模拟发币
}
```

### StyleMarketIndex.vue（新建）

- 顶部统计：市场风格总数、本周被使用总次数。
- 搜索框：按风格名 / 适用范围过滤。
- 卡片网格：展示风格名、创作者、适用范围、本周使用次数、累计使用次数、价格。
- 操作：「使用」按钮（余额不足时禁用）。

### EarningsIndex.vue（新建）

- 顶部卡片：总收益、本周收益、本周被使用次数。
- 收益明细列表：时间、风格、类型、金额、描述。

### StylesIndex.vue 改动

- 我的风格 / 学习的风格卡片增加「分享」按钮。
- 已分享但待审核的风格显示「审核中」标签，并提供「模拟通过」按钮（演示用）。
- 已上架风格显示「已上架」标签。

### ConsoleLayout.vue 改动

- 侧边栏「我的风格」展开为子菜单：
  - 我的风格
  - 风格市场
  - 收益明细

### router/index.js 改动

- 新增 `/console/style-market`
- 新增 `/console/earnings`

### CreateIndex.vue 改动

- 页面加载时读取 `marketStyleId` query 参数。
- 自动应用对应市场风格到当前创作。

## 5. 数据流

```
用户 A 在「我的风格」点击「分享」
  ↓
shareStyleToMarket(style, 'my') → aichuangzuo_style_market 新增记录（status=pending）
  ↓
（演示）点击「模拟通过」→ status=approved
  ↓
用户 B 进入「风格市场」浏览并点击「使用」
  ↓
useMarketStyle(id, userBId) → 扣 B 0.2 币，给 A +0.2 币，weeklyUses++
  ↓
跳转 /console/create?marketStyleId=xxx 并应用风格
  ↓
周日 23:59 settleWeeklyMilestone() → 按周使用次数发里程碑奖励
```

## 6. 计费与奖励规则

### 基础计费

- 每次使用扣 0.2 创作币。
- 创作者实时获得 0.2 创作币。
- 余额不足时禁用「使用」按钮。

### 里程碑奖励（自然周结算）

| 本周使用次数 | 额外奖励 |
|---|---|
| 50-199 | +5 创作币 |
| 200-499 | +15 创作币 |
| 500-999 | +30 创作币 |
| 1000+ | +60 创作币 |

- 每周日 23:59 自动结算。
- 只取最高档发放一次，不累加。
- 结算后 `weeklyUses` 清零，`milestoneBonus` 归零。

### 示例

- 风格 A 本周被用 80 次 → 基础 16 币 + 里程碑 5 币 = 21 币。
- 风格 B 本周被用 600 次 → 基础 120 币 + 里程碑 30 币 = 150 币。

## 7. 边界与错误处理

| 场景 | 处理 |
|---|---|
| 余额不足 0.2 | 「使用」按钮禁用，提示「余额不足」 |
| 重复分享同一风格 | 已分享过且未下架则提示「已分享」 |
| 未审核通过 | 市场列表只展示 `approved` 状态 |
| 结算时无使用 | 不发放奖励，直接清零 |
| 跨周使用 | 按 `lastSettlementAt` 判断，新周重新累计 |

## 8. 测试要点

1. **分享风格到市场**：在我的风格/学习的风格卡片点击「分享」→ 状态变为「审核中」。
2. **模拟审核通过**：点击「模拟通过」→ 风格出现在市场页。
3. **市场发现**：进入市场页，能看到已上架风格、搜索、价格。
4. **使用扣费**：使用市场风格 → 余额 -0.2，创作者收益 +0.2。
5. **余额不足**：余额 < 0.2 时「使用」按钮禁用。
6. **里程碑结算**：模拟使用 50 次 → 周日结算后发放 +5 币。
7. **收益明细**：收益页显示总收益、本周收益、收益记录列表。
8. **创作页联动**：市场页点击「使用」跳转创作页并应用该风格。

## 9. 实现位置汇总

| 文件 | 操作 |
|---|---|
| `project/user/web/src/composables/useStyleMarket.js` | 新建：市场状态、分享、使用、结算、收益 |
| `project/user/web/src/views/console/StyleMarketIndex.vue` | 新建：市场发现页 |
| `project/user/web/src/views/console/EarningsIndex.vue` | 新建：收益明细页 |
| `project/user/web/src/composables/useStyles.js` | 修改：分享时复制风格到市场 |
| `project/user/web/src/views/console/StylesIndex.vue` | 修改：卡片增加「分享」与审核状态 |
| `project/user/web/src/views/console/ConsoleLayout.vue` | 修改：导航子菜单 |
| `project/user/web/src/router/index.js` | 修改：新增路由 |
| `project/user/web/src/views/console/CreateIndex.vue` | 修改：支持 marketStyleId 参数 |
