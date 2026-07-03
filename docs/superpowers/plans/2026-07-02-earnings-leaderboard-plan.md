# 收益排行榜 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Vue 控制台新增「收益排行榜」页面，包含创作币月度榜、自媒体收入月/年度榜、收入申报与模拟审核、月度前 10 名自动奖励 100 创作币。

**Architecture:** 新建 `useLeaderboard.js` 负责榜单数据生成、申报记录、奖励发放的 localStorage 状态管理；新建 `LeaderboardIndex.vue` 作为页面入口，内部按 Tab 拆分「创作币榜」「自媒体收入榜」两个视图；修改 `ConsoleLayout.vue` 与 `router/index.js` 增加菜单与路由。

**Tech Stack:** Vue 3 (Composition API), Ant Design Vue, Vue Router 4, Vite, localStorage, Playwright (验证)。

## Global Constraints

- 仅在 `project/user/web/` 内实现，纯前端 localStorage mock，不接入后端。
- 颜色、圆角、按钮等视觉规范遵循 `docs/design/design-system.md`，主色 `#ff2442`。
- 暗色主题通过 `body[data-theme="dark"]` 选择器覆盖，保持与现有控制台页面一致。
- 所有新增 localStorage key 以 `aichuangzuo_` 开头。
- 金额统一保留两位小数，localStorage 存字符串避免浮点问题。
- 图片上传使用 Base64，限制 5MB，仅 jpg/png。
- 奖励发放记录以 `leaderboardType + periodValue + userId` 去重，避免重复发放。

---

## File Structure

| 文件 | 职责 |
|---|---|
| `project/user/web/src/composables/useLeaderboard.js` | 榜单核心状态与逻辑：mock 用户生成、创作币榜聚合、收入申报 CRUD、模拟审核、奖励发放与去重。 |
| `project/user/web/src/views/console/LeaderboardIndex.vue` | 页面 UI：标题区、Tab 切换、周期选择、榜单渲染、申报弹框、规则弹框、我的申报记录。 |
| `project/user/web/src/views/console/ConsoleLayout.vue` | 修改：侧边栏新增「收益排行榜」菜单项，引入 `TrophyOutlined`。 |
| `project/user/web/src/router/index.js` | 修改：新增 `/console/leaderboard` 子路由。 |
| `project/user/web/src/composables/useStyleMarket.js` | 可选修改：奖励发放时写入 `earningsRecords` 类型 `leaderboard_reward`。 |
| `project/user/web/src/views/console/EarningsIndex.vue` | 可选修改：`formatType` 增加 `leaderboard_reward` 显示。 |
| `tests/e2e/verify_leaderboard.py` | Playwright 端到端验证脚本。 |

---

## Task 1: 创建 useLeaderboard.js 基础骨架

**Files:**
- Create: `project/user/web/src/composables/useLeaderboard.js`
- Test: 浏览器控制台手动验证 `localStorage` 读写

**Interfaces:**
- Produces: `incomeSubmissions`, `rewardRecords`, `getCoinLeaderboard(month)`, `getIncomeLeaderboard(periodType, periodValue)`, `submitIncomeSubmission(payload)`, `approveIncomeSubmission(id)`, `rejectIncomeSubmission(id, reason)`, `maybeAwardMonthlyRewards()`, `simulateAwardMonthlyRewards(periodValue)`

- [ ] **Step 1: 编写 localStorage 加载/保存与常量**

```js
import { ref } from 'vue'

const INCOME_SUBMISSIONS_KEY = 'aichuangzuo_leaderboard_income_submissions'
const REWARD_RECORDS_KEY = 'aichuangzuo_leaderboard_rewards'
const USER_ID_KEY = 'aichuangzuo_user_id'
const COIN_BALANCE_KEY = 'aichuangzuo_coin_balance'
const EARNINGS_KEY = 'aichuangzuo_earnings_records'

function load(key, fallback = []) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch {
    return fallback
  }
}

function save(key, value) {
  localStorage.setItem(key, JSON.stringify(value))
}

export function getUserId() {
  let id = localStorage.getItem(USER_ID_KEY)
  if (!id) {
    id = 'u_' + Math.random().toString(36).slice(2, 10)
    localStorage.setItem(USER_ID_KEY, id)
  }
  return id
}

export const incomeSubmissions = ref(load(INCOME_SUBMISSIONS_KEY))
export const rewardRecords = ref(load(REWARD_RECORDS_KEY))
```

- [ ] **Step 2: 运行验证**

在浏览器控制台执行：

```js
localStorage.clear()
import('/src/composables/useLeaderboard.js').then(m => console.log(m.getUserId(), m.incomeSubmissions.value, m.rewardRecords.value))
```

Expected: 输出用户 ID、空数组、空数组，且 `localStorage` 出现 `aichuangzuo_user_id`。

- [ ] **Step 3: Commit**

```bash
git add project/user/web/src/composables/useLeaderboard.js
git commit -m "feat(leaderboard): 创建 useLeaderboard 基础骨架与 localStorage 读写"
```

---

## Task 2: 实现创作币榜 mock 用户生成与排名

**Files:**
- Modify: `project/user/web/src/composables/useLeaderboard.js`
- Test: 浏览器控制台调用 `getCoinLeaderboard`

**Interfaces:**
- Consumes: `earningsRecords` from `@/composables/useStyleMarket.js`, `getUserId()`
- Produces: `getCoinLeaderboard(month)` returns `[{ rank, userId, nickname, amount, isMe }]`

- [ ] **Step 1: 编写测试脚本**

临时在 `project/user/web/src/composables/useLeaderboard.js` 底部加入（实现后删除）：

```js
// 临时自测
window.testCoin = () => console.log(getCoinLeaderboard('2026-07'))
```

- [ ] **Step 2: 实现确定性 mock 用户生成函数**

```js
const MOCK_NICKNAMES = [
  '创作者小王', '文案阿杰', '自媒体老李', '写作喵', '内容工匠',
  '运营小周', '爆款制造机', '深夜写手', '追光者', '灵感捕手',
  '笔尖流浪', '文章猎手', '流量玩家', '创作旅人', '码字工',
  '新媒体小白', '观点输出机', '故事银行', '热榜观察员'
]

function hashString(str) {
  let h = 0
  for (let i = 0; i < str.length; i++) {
    h = (h << 5) - h + str.charCodeAt(i)
    h |= 0
  }
  return Math.abs(h)
}

function seededRandom(seed) {
  const x = Math.sin(seed) * 10000
  return x - Math.floor(x)
}

function generateMockUser(index, month) {
  const userId = 'mock_' + index
  const nickname = MOCK_NICKNAMES[index % MOCK_NICKNAMES.length]
  const seed = hashString(`${month}-${userId}`)
  const amount = Number((seededRandom(seed) * 50 + 0.5).toFixed(2))
  return { userId, nickname, amount }
}
```

- [ ] **Step 3: 实现 getCoinLeaderboard**

```js
import { earningsRecords } from '@/composables/useStyleMarket.js'

export function getCoinLeaderboard(month) {
  const currentUserId = getUserId()
  const start = new Date(`${month}-01T00:00:00`)
  const end = new Date(start.getFullYear(), start.getMonth() + 1, 1)

  const currentUserAmount = earningsRecords.value
    .filter(r => {
      if (!r.createdAt) return false
      const d = new Date(r.createdAt)
      return r.fromUserId === currentUserId && r.amount > 0 && d >= start && d < end
    })
    .reduce((sum, r) => sum + r.amount, 0)

  const list = [
    { userId: currentUserId, nickname: '我', amount: Number(currentUserAmount.toFixed(2)), isMe: true },
    ...MOCK_NICKNAMES.map((_, i) => generateMockUser(i, month))
  ]

  list.sort((a, b) => b.amount - a.amount)
  return list.map((item, index) => ({ ...item, rank: index + 1 }))
}
```

- [ ] **Step 4: 运行验证**

浏览器控制台执行 `window.testCoin()`。

Expected: 输出 20 条记录，第一条 rank 为 1，包含 `isMe` 字段，金额非负。

- [ ] **Step 5: 删除临时自测代码并 Commit**

```bash
git add project/user/web/src/composables/useLeaderboard.js
git commit -m "feat(leaderboard): 创作币榜聚合与 mock 用户生成"
```

---

## Task 3: 实现自媒体收入榜与申报记录

**Files:**
- Modify: `project/user/web/src/composables/useLeaderboard.js`
- Test: 浏览器控制台调用提交/通过/拒绝函数

**Interfaces:**
- Produces: `submitIncomeSubmission(payload)`, `approveIncomeSubmission(id)`, `rejectIncomeSubmission(id, reason)`, `getIncomeLeaderboard(periodType, periodValue)`

- [ ] **Step 1: 编写申报 CRUD 函数**

```js
export function submitIncomeSubmission(payload) {
  const submission = {
    id: 'income-' + Date.now().toString(36),
    userId: getUserId(),
    month: payload.month,
    amount: Number(payload.amount),
    screenshot: payload.screenshot,
    status: 'pending',
    createdAt: new Date().toISOString(),
    auditedAt: '',
    rejectReason: ''
  }
  incomeSubmissions.value.unshift(submission)
  save(INCOME_SUBMISSIONS_KEY, incomeSubmissions.value)
  return submission.id
}

export function approveIncomeSubmission(id) {
  const s = incomeSubmissions.value.find(x => x.id === id)
  if (s && s.status === 'pending') {
    s.status = 'approved'
    s.auditedAt = new Date().toISOString()
    save(INCOME_SUBMISSIONS_KEY, incomeSubmissions.value)
  }
}

export function rejectIncomeSubmission(id, reason) {
  const s = incomeSubmissions.value.find(x => x.id === id)
  if (s && s.status === 'pending') {
    s.status = 'rejected'
    s.rejectReason = reason || ''
    s.auditedAt = new Date().toISOString()
    save(INCOME_SUBMISSIONS_KEY, incomeSubmissions.value)
  }
}

export function getMyIncomeSubmissions() {
  return incomeSubmissions.value.filter(s => s.userId === getUserId())
}
```

- [ ] **Step 2: 实现 getIncomeLeaderboard**

```js
function getPeriodRange(periodType, periodValue) {
  if (periodType === 'month') {
    const start = new Date(`${periodValue}-01T00:00:00`)
    const end = new Date(start.getFullYear(), start.getMonth() + 1, 1)
    return { start, end }
  }
  const year = parseInt(periodValue, 10)
  return { start: new Date(`${year}-01-01T00:00:00`), end: new Date(`${year + 1}-01-01T00:00:00`) }
}

function generateMockIncomeUser(index, periodType, periodValue, baseAmount) {
  const userId = 'mock_income_' + index
  const nickname = MOCK_NICKNAMES[(index + 5) % MOCK_NICKNAMES.length]
  const seed = hashString(`${periodType}-${periodValue}-${userId}`)
  const factor = periodType === 'year' ? 12 : 1
  const amount = Number(((seededRandom(seed) * 10000 + 500) * factor).toFixed(2))
  return { userId, nickname, amount }
}

export function getIncomeLeaderboard(periodType, periodValue) {
  const currentUserId = getUserId()
  const { start, end } = getPeriodRange(periodType, periodValue)

  const currentUserAmount = incomeSubmissions.value
    .filter(s => {
      if (s.status !== 'approved' || s.userId !== currentUserId) return false
      const d = new Date(`${s.month}-01T00:00:00`)
      return d >= start && d < end
    })
    .reduce((sum, s) => sum + s.amount, 0)

  const list = [
    { userId: currentUserId, nickname: '我', amount: Number(currentUserAmount.toFixed(2)), isMe: true }
  ]

  for (let i = 0; i < 10; i++) {
    list.push(generateMockIncomeUser(i, periodType, periodValue, currentUserAmount))
  }

  list.sort((a, b) => b.amount - a.amount)
  return list.map((item, index) => ({ ...item, rank: index + 1 }))
}
```

- [ ] **Step 3: 运行验证**

浏览器控制台执行：

```js
import('/src/composables/useLeaderboard.js').then(m => {
  const id = m.submitIncomeSubmission({ month: '2026-07', amount: 3000, screenshot: 'data:image/png;base64,xxx' })
  console.log('submitted', id)
  console.log('pending', m.getIncomeLeaderboard('month', '2026-07'))
  m.approveIncomeSubmission(id)
  console.log('approved', m.getIncomeLeaderboard('month', '2026-07'))
})
```

Expected: 提交后当前用户金额为 0；通过后金额变为 3000；年度榜聚合正确。

- [ ] **Step 4: Commit**

```bash
git add project/user/web/src/composables/useLeaderboard.js
git commit -m "feat(leaderboard): 自媒体收入申报与榜单聚合"
```

---

## Task 4: 实现月度奖励发放逻辑

**Files:**
- Modify: `project/user/web/src/composables/useLeaderboard.js`
- Modify: `project/user/web/src/composables/useStyleMarket.js`（写入 earningsRecords）
- Test: 浏览器控制台调用 `simulateAwardMonthlyRewards`

**Interfaces:**
- Produces: `maybeAwardMonthlyRewards()`, `simulateAwardMonthlyRewards(periodValue)`
- Produces: earnings record with type `leaderboard_reward`

- [ ] **Step 1: 实现奖励发放与去重**

```js
function getCurrentMonth() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

function parseMonth(month) {
  const [y, m] = month.split('-').map(Number)
  return new Date(y, m - 1, 1)
}

function addMonths(month, delta) {
  const d = parseMonth(month)
  d.setMonth(d.getMonth() + delta)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

function hasRewardAwarded(leaderboardType, periodValue, userId) {
  return rewardRecords.value.some(
    r => r.leaderboardType === leaderboardType && r.periodValue === periodValue && r.userId === userId
  )
}

function awardTopUsers(leaderboardType, periodValue, getLeaderboardFn) {
  const list = getLeaderboardFn()
  const top10 = list.slice(0, 10)
  let awardedCount = 0

  top10.forEach(item => {
    if (hasRewardAwarded(leaderboardType, periodValue, item.userId)) return

    const record = {
      periodType: 'month',
      periodValue,
      leaderboardType,
      rank: item.rank,
      userId: item.userId,
      amount: 100,
      awardedAt: new Date().toISOString()
    }
    rewardRecords.value.unshift(record)

    if (item.isMe) {
      const balance = parseFloat(localStorage.getItem(COIN_BALANCE_KEY) || '0')
      localStorage.setItem(COIN_BALANCE_KEY, String(Number((balance + 100).toFixed(2))))

      const earnings = load(EARNINGS_KEY, [])
      earnings.unshift({
        id: 'earn-' + Date.now().toString(36),
        type: 'leaderboard_reward',
        styleName: '',
        styleId: '',
        amount: 100,
        description: `${leaderboardType === 'coin' ? '创作币榜' : '自媒体收入榜'} 月度第 ${item.rank} 名奖励`,
        status: 'settled',
        settlementWeek: '',
        createdAt: new Date().toISOString()
      })
      save(EARNINGS_KEY, earnings)
    }

    awardedCount++
  })

  if (awardedCount > 0) {
    save(REWARD_RECORDS_KEY, rewardRecords.value)
  }
  return awardedCount
}

export function simulateAwardMonthlyRewards(periodValue) {
  awardTopUsers('coin', periodValue, () => getCoinLeaderboard(periodValue))
  awardTopUsers('income', periodValue, () => getIncomeLeaderboard('month', periodValue))
}

export function maybeAwardMonthlyRewards() {
  const currentMonth = getCurrentMonth()
  const lastMonth = addMonths(currentMonth, -1)
  simulateAwardMonthlyRewards(lastMonth)
}
```

- [ ] **Step 2: 在 useStyleMarket.js 增加 leaderboard_reward 类型识别**

```js
// project/user/web/src/composables/useStyleMarket.js
// 在 earningsRecords 加载处，迁移旧数据时允许 type === 'leaderboard_reward'
```

无需额外代码，加载逻辑 `JSON.parse` 已兼容。

- [ ] **Step 3: 运行验证**

浏览器控制台执行：

```js
import('/src/composables/useLeaderboard.js').then(m => {
  const before = parseFloat(localStorage.getItem('aichuangzuo_coin_balance') || '0')
  m.simulateAwardMonthlyRewards('2026-06')
  const after = parseFloat(localStorage.getItem('aichuangzuo_coin_balance') || '0')
  console.log('balance delta', after - before)
  console.log('rewards', JSON.parse(localStorage.getItem('aichuangzuo_leaderboard_rewards')))
})
```

Expected: 如果当前用户在 2026-06 创作币榜或自媒体收入榜前 10，余额增加 100（每个榜单最多一次）。

- [ ] **Step 4: Commit**

```bash
git add project/user/web/src/composables/useLeaderboard.js
git commit -m "feat(leaderboard): 月度榜单奖励发放与去重逻辑"
```

---

## Task 5: 新增侧边栏菜单与路由

**Files:**
- Modify: `project/user/web/src/views/console/ConsoleLayout.vue`
- Modify: `project/user/web/src/router/index.js`
- Test: 浏览器访问 `/console/leaderboard`

**Interfaces:**
- Produces: route `/console/leaderboard`, sidebar menu item

- [ ] **Step 1: 在 ConsoleLayout.vue 引入图标并新增菜单**

```js
// 在已有的 import 区域加入
import { TrophyOutlined } from '@ant-design/icons-vue'

// 在 navItems 数组中新增一项
const navItems = [
  { path: '/console/create', label: '创作', icon: EditOutlined },
  { path: '/console/works', label: '我的作品', icon: FolderOutlined },
  { path: '/console/styles', label: '我的风格', icon: SmileOutlined },
  { path: '/console/style-market', label: '风格市场', icon: ShopOutlined },
  { path: '/console/earnings', label: '我的账户', icon: DollarOutlined },
  { path: '/console/hot-search', label: '热搜榜', icon: FireOutlined },
  { path: '/console/leaderboard', label: '收益排行榜', icon: TrophyOutlined }
]
```

- [ ] **Step 2: 在 router/index.js 新增路由**

```js
{
  path: 'leaderboard',
  name: 'ConsoleLeaderboard',
  component: () => import('@/views/console/LeaderboardIndex.vue')
}
```

- [ ] **Step 3: 临时创建 LeaderboardIndex.vue 空壳**

```vue
<template>
  <div class="leaderboard-page">收益排行榜占位</div>
</template>
```

- [ ] **Step 4: 运行验证**

```bash
cd project/user/web
npm run dev
```

浏览器访问 `http://localhost:5173/console/leaderboard`（端口以实际为准）。

Expected: 侧边栏出现「收益排行榜」，点击进入后页面显示「收益排行榜占位」。

- [ ] **Step 5: Commit**

```bash
git add project/user/web/src/views/console/ConsoleLayout.vue project/user/web/src/router/index.js project/user/web/src/views/console/LeaderboardIndex.vue
git commit -m "feat(leaderboard): 新增收益排行榜菜单与路由"
```

---

## Task 6: 创建 LeaderboardIndex.vue 页面骨架

**Files:**
- Modify: `project/user/web/src/views/console/LeaderboardIndex.vue`
- Test: 浏览器切换 Tab、选择月份

**Interfaces:**
- Consumes: `getCoinLeaderboard`, `getIncomeLeaderboard`

- [ ] **Step 1: 实现页面标题区与 Tab 切换**

```vue
<template>
  <div class="leaderboard-page">
    <div class="leaderboard-header">
      <div>
        <h2 class="leaderboard-title">收益排行榜</h2>
        <p class="leaderboard-subtitle">查看创作激励排名，冲击月度榜单奖励</p>
      </div>
      <span class="leaderboard-rules-link" @click="rulesVisible = true">榜单规则</span>
    </div>

    <div class="leaderboard-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="['leaderboard-tab', { active: activeTab === tab.key }]"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-if="activeTab === 'coin'" class="leaderboard-section">创作币榜内容区</div>
    <div v-else class="leaderboard-section">自媒体收入榜内容区</div>

    <a-modal
      v-model:open="rulesVisible"
      title="榜单规则"
      :footer="null"
      :width="560"
      centered
      class="leaderboard-rules-modal"
    >
      <ol class="leaderboard-rules-list">
        <li>...</li>
      </ol>
    </a-modal>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const activeTab = ref('coin')
const rulesVisible = ref(false)

const tabs = [
  { key: 'coin', label: '创作币榜' },
  { key: 'income', label: '自媒体收入榜' }
]
</script>

<style scoped>
.leaderboard-page {
  height: 100%;
  padding: 24px;
  overflow-y: auto;
}
.leaderboard-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}
.leaderboard-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px;
}
.leaderboard-subtitle {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}
.leaderboard-rules-link {
  color: #ff2442;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  text-decoration: underline;
  text-underline-offset: 3px;
}
.leaderboard-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 8px;
  width: fit-content;
}
.leaderboard-tab {
  padding: 8px 20px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}
.leaderboard-tab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}
</style>
```

- [ ] **Step 2: 运行验证**

浏览器中切换 Tab，确认两个 Tab 内容区文字变化。

- [ ] **Step 3: Commit**

```bash
git add project/user/web/src/views/console/LeaderboardIndex.vue
git commit -m "feat(leaderboard): 页面标题、Tab 切换与规则弹框骨架"
```

---

## Task 7: 实现创作币榜视图

**Files:**
- Modify: `project/user/web/src/views/console/LeaderboardIndex.vue`
- Test: 浏览器查看创作币榜排名、切换月份

**Interfaces:**
- Consumes: `getCoinLeaderboard`

- [ ] **Step 1: 添加月份选择器与创作币榜列表**

在 `leaderboard-tabs` 下方插入：

```vue
<div v-if="activeTab === 'coin'" class="leaderboard-section">
  <div class="leaderboard-toolbar">
    <select v-model="coinMonth" class="leaderboard-select">
      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}</option>
    </select>
  </div>

  <div class="leaderboard-list">
    <div
      v-for="item in coinList"
      :key="item.userId"
      :class="['leaderboard-item', { 'is-me': item.isMe }, 'rank-' + item.rank]"
    >
      <span class="leaderboard-rank">{{ item.rank }}</span>
      <span class="leaderboard-avatar">{{ item.nickname.charAt(0) }}</span>
      <span class="leaderboard-nickname">{{ item.nickname }}</span>
      <span v-if="item.isMe" class="leaderboard-me-tag">我</span>
      <span class="leaderboard-amount">{{ item.amount.toFixed(2) }} 创作币</span>
    </div>
  </div>
</div>
```

- [ ] **Step 2: 在 script 中引入计算属性**

```js
import { ref, computed } from 'vue'
import { getCoinLeaderboard, getIncomeLeaderboard } from '@/composables/useLeaderboard.js'

function getMonthOptions() {
  const options = []
  const now = new Date()
  for (let i = 0; i < 12; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    options.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
  }
  return options
}

const coinMonth = ref(getMonthOptions()[0])
const monthOptions = getMonthOptions()
const coinList = computed(() => getCoinLeaderboard(coinMonth.value))
```

- [ ] **Step 3: 添加基础样式**

```css
.leaderboard-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}
.leaderboard-select {
  height: 36px;
  padding: 0 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
}
.leaderboard-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.leaderboard-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
}
.leaderboard-item.is-me {
  background: #fff0f2;
  border-color: #ffd1d9;
}
.leaderboard-rank {
  width: 28px;
  text-align: center;
  font-weight: 700;
  color: #8c8c8c;
}
.leaderboard-item.rank-1 .leaderboard-rank { color: #cf1322; }
.leaderboard-item.rank-2 .leaderboard-rank { color: #d48806; }
.leaderboard-item.rank-3 .leaderboard-rank { color: #389e0d; }
.leaderboard-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #595959;
}
.leaderboard-nickname {
  flex: 1;
  font-size: 15px;
  color: #1a1a1a;
}
.leaderboard-me-tag {
  font-size: 11px;
  padding: 2px 8px;
  background: #ff2442;
  color: #fff;
  border-radius: 10px;
}
.leaderboard-amount {
  font-size: 15px;
  font-weight: 600;
  color: #ff2442;
}
```

- [ ] **Step 4: 运行验证**

切换月份，确认当前用户排名变化；确认前 3 名 rank 颜色不同。

- [ ] **Step 5: Commit**

```bash
git add project/user/web/src/views/console/LeaderboardIndex.vue
git commit -m "feat(leaderboard): 创作币榜月度排名展示"
```

---

## Task 8: 实现前 3 名突出卡片与奖励标签

**Files:**
- Modify: `project/user/web/src/views/console/LeaderboardIndex.vue`
- Modify: `project/user/web/src/composables/useLeaderboard.js`
- Test: 浏览器查看前三名样式与奖励标签

**Interfaces:**
- Produces: `isRewardAwarded(leaderboardType, periodValue, rank)` or `getRewardRecord(...)`

- [ ] **Step 1: 在 useLeaderboard.js 添加奖励查询函数**

```js
export function getRewardRecord(leaderboardType, periodValue, userId) {
  return rewardRecords.value.find(
    r => r.leaderboardType === leaderboardType && r.periodValue === periodValue && r.userId === userId
  )
}
```

- [ ] **Step 2: 在 LeaderboardIndex.vue 增加前三名卡片渲染**

将列表渲染改为：

```vue
<div class="leaderboard-top3">
  <div
    v-for="item in coinTop3"
    :key="item.userId"
    :class="['leaderboard-top-card', 'top-' + item.rank, { 'is-me': item.isMe }]"
  >
    <div class="top-rank">{{ item.rank }}</div>
    <div class="top-nickname">{{ item.nickname }}</div>
    <div class="top-amount">{{ item.amount.toFixed(2) }} 创作币</div>
    <div v-if="item.isMe" class="top-me-tag">我</div>
    <div v-if="coinRewardLabel(item)" :class="['top-reward', coinRewardLabel(item).type]">
      {{ coinRewardLabel(item).text }}
    </div>
  </div>
</div>

<div class="leaderboard-list">
  <div
    v-for="item in coinListAfter3"
    :key="item.userId"
    :class="['leaderboard-item', { 'is-me': item.isMe }]"
  >
    <span class="leaderboard-rank">{{ item.rank }}</span>
    <span class="leaderboard-avatar">{{ item.nickname.charAt(0) }}</span>
    <span class="leaderboard-nickname">{{ item.nickname }}</span>
    <span v-if="item.isMe" class="leaderboard-me-tag">我</span>
    <div v-if="coinRewardLabel(item)" :class="['leaderboard-reward', coinRewardLabel(item).type]">
      {{ coinRewardLabel(item).text }}
    </div>
    <span class="leaderboard-amount">{{ item.amount.toFixed(2) }} 创作币</span>
  </div>
</div>
```

- [ ] **Step 3: 在 script 中增加计算属性与奖励标签逻辑**

```js
import { getRewardRecord } from '@/composables/useLeaderboard.js'

const coinTop3 = computed(() => coinList.value.slice(0, 3))
const coinListAfter3 = computed(() => coinList.value.slice(3))

function coinRewardLabel(item) {
  const isCurrentMonth = coinMonth.value === getMonthOptions()[0]
  if (item.rank > 10) return null
  const record = getRewardRecord('coin', coinMonth.value, item.userId)
  if (record) return { text: '已获 100 创作币', type: 'awarded' }
  if (isCurrentMonth) return { text: '本月榜单进行中，待结算', type: 'pending' }
  return null
}
```

- [ ] **Step 4: 添加前 3 名卡片样式**

```css
.leaderboard-top3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.leaderboard-top-card {
  position: relative;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  padding: 24px 16px;
  text-align: center;
}
.leaderboard-top-card.top-1 {
  background: linear-gradient(180deg, #fff7e6 0%, #fff 100%);
  border-color: #ffd591;
}
.leaderboard-top-card.top-2 {
  background: linear-gradient(180deg, #f6ffed 0%, #fff 100%);
  border-color: #b7eb8f;
}
.leaderboard-top-card.top-3 {
  background: linear-gradient(180deg, #e6f7ff 0%, #fff 100%);
  border-color: #91d5ff;
}
.top-rank {
  font-size: 28px;
  font-weight: 700;
  color: #d48806;
  margin-bottom: 8px;
}
.top-nickname {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}
.top-amount {
  font-size: 18px;
  font-weight: 700;
  color: #ff2442;
}
.top-me-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  font-size: 11px;
  padding: 2px 8px;
  background: #ff2442;
  color: #fff;
  border-radius: 10px;
}
.leaderboard-reward, .top-reward {
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 10px;
  font-weight: 500;
}
.leaderboard-reward.awarded, .top-reward.awarded {
  background: #f6ffed;
  color: #389e0d;
}
.leaderboard-reward.pending, .top-reward.pending {
  background: #f5f5f5;
  color: #8c8c8c;
}
```

- [ ] **Step 5: 运行验证**

确认前 3 名以卡片形式展示，第 4 名起为列表；当前月 TOP 10 显示「待结算」。

- [ ] **Step 6: Commit**

```bash
git add project/user/web/src/views/console/LeaderboardIndex.vue project/user/web/src/composables/useLeaderboard.js
git commit -m "feat(leaderboard): 前三名卡片样式与月度奖励标签"
```

---

## Task 9: 实现自媒体收入榜视图

**Files:**
- Modify: `project/user/web/src/views/console/LeaderboardIndex.vue`
- Test: 浏览器切换月/年度、查看收入榜

**Interfaces:**
- Consumes: `getIncomeLeaderboard`, `getMyIncomeSubmissions`, `submitIncomeSubmission`, `approveIncomeSubmission`, `rejectIncomeSubmission`, `simulateAwardMonthlyRewards`

- [ ] **Step 1: 添加周期切换与收入榜渲染**

```vue
<div v-else class="leaderboard-section">
  <div class="leaderboard-toolbar">
    <div class="leaderboard-period-tabs">
      <button
        v-for="p in incomePeriods"
        :key="p.key"
        :class="['leaderboard-period-tab', { active: incomePeriodType === p.key }]"
        @click="incomePeriodType = p.key"
      >
        {{ p.label }}
      </button>
    </div>
    <select v-if="incomePeriodType === 'month'" v-model="incomeMonth" class="leaderboard-select">
      <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}</option>
    </select>
    <select v-else v-model="incomeYear" class="leaderboard-select">
      <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}</option>
    </select>
    <button class="leaderboard-action-btn" @click="openIncomeModal">申报收入</button>
  </div>

  <div class="leaderboard-top3">
    <div
      v-for="item in incomeTop3"
      :key="item.userId"
      :class="['leaderboard-top-card', 'top-' + item.rank, { 'is-me': item.isMe }]"
    >
      <div class="top-rank">{{ item.rank }}</div>
      <div class="top-nickname">{{ item.nickname }}</div>
      <div class="top-amount">{{ item.amount.toFixed(2) }} 元</div>
      <div v-if="item.isMe" class="top-me-tag">我</div>
      <div v-if="incomeRewardLabel(item)" :class="['top-reward', incomeRewardLabel(item).type]">
        {{ incomeRewardLabel(item).text }}
      </div>
    </div>
  </div>

  <div class="leaderboard-list">
    <div
      v-for="item in incomeListAfter3"
      :key="item.userId"
      :class="['leaderboard-item', { 'is-me': item.isMe }]"
    >
      <span class="leaderboard-rank">{{ item.rank }}</span>
      <span class="leaderboard-avatar">{{ item.nickname.charAt(0) }}</span>
      <span class="leaderboard-nickname">{{ item.nickname }}</span>
      <span v-if="item.isMe" class="leaderboard-me-tag">我</span>
      <div v-if="incomeRewardLabel(item)" :class="['leaderboard-reward', incomeRewardLabel(item).type]">
        {{ incomeRewardLabel(item).text }}
      </div>
      <span class="leaderboard-amount">{{ item.amount.toFixed(2) }} 元</span>
    </div>
  </div>

  <div class="leaderboard-submissions">
    <div class="leaderboard-submissions-header">
      <span>我的申报记录</span>
      <button class="leaderboard-action-btn secondary" @click="simulateAwardMonthlyRewards(getCurrentMonth(-1))">
        模拟发放上月奖励
      </button>
    </div>
    <div v-if="mySubmissions.length === 0" class="leaderboard-empty">暂无申报记录</div>
    <div v-else class="leaderboard-submission-list">
      <div v-for="s in mySubmissions" :key="s.id" class="leaderboard-submission-item">
        <div>
          <div class="submission-month">{{ s.month }}</div>
          <div class="submission-amount">{{ s.amount.toFixed(2) }} 元</div>
        </div>
        <div class="submission-right">
          <img v-if="s.screenshot" :src="s.screenshot" class="submission-thumb" />
          <span :class="['submission-status', s.status]">{{ statusText(s.status) }}</span>
          <button v-if="s.status === 'pending'" class="submission-btn pass" @click="approveSubmission(s.id)">通过</button>
          <button v-if="s.status === 'pending'" class="submission-btn reject" @click="rejectSubmission(s.id)">拒绝</button>
        </div>
      </div>
    </div>
  </div>
</div>
```

- [ ] **Step 2: 在 script 中增加状态与逻辑**

```js
import {
  getIncomeLeaderboard,
  getMyIncomeSubmissions,
  submitIncomeSubmission,
  approveIncomeSubmission,
  rejectIncomeSubmission,
  simulateAwardMonthlyRewards,
  getRewardRecord
} from '@/composables/useLeaderboard.js'

const incomePeriodType = ref('month')
const incomeMonth = ref(monthOptions[0])
const incomeYear = ref(new Date().getFullYear())
const incomePeriods = [
  { key: 'month', label: '月度榜' },
  { key: 'year', label: '年度榜' }
]
const yearOptions = Array.from({ length: 3 }, (_, i) => new Date().getFullYear() - i)

const incomeList = computed(() => {
  const value = incomePeriodType.value === 'month' ? incomeMonth.value : String(incomeYear.value)
  return getIncomeLeaderboard(incomePeriodType.value, value)
})
const incomeTop3 = computed(() => incomeList.value.slice(0, 3))
const incomeListAfter3 = computed(() => incomeList.value.slice(3))
const mySubmissions = computed(() => getMyIncomeSubmissions())

function incomeRewardLabel(item) {
  if (incomePeriodType.value !== 'month') return null
  if (item.rank > 10) return null
  const record = getRewardRecord('income', incomeMonth.value, item.userId)
  if (record) return { text: '已获 100 创作币', type: 'awarded' }
  if (incomeMonth.value === monthOptions[0]) return { text: '本月榜单进行中，待结算', type: 'pending' }
  return null
}

function statusText(status) {
  return { pending: '审核中', approved: '已通过', rejected: '已拒绝' }[status] || status
}

function approveSubmission(id) {
  approveIncomeSubmission(id)
}

function rejectSubmission(id) {
  rejectIncomeSubmission(id, '截图不清晰')
}

function getCurrentMonth(offset = 0) {
  const d = new Date()
  d.setMonth(d.getMonth() + offset)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}
```

- [ ] **Step 3: 添加工具栏与申报记录样式**

```css
.leaderboard-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.leaderboard-period-tabs {
  display: flex;
  gap: 4px;
  background: #f5f5f5;
  padding: 4px;
  border-radius: 8px;
}
.leaderboard-period-tab {
  padding: 6px 14px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
}
.leaderboard-period-tab.active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}
.leaderboard-action-btn {
  padding: 8px 16px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}
.leaderboard-action-btn.secondary {
  background: #fff;
  color: #ff2442;
  border: 1px solid #ff2442;
}
.leaderboard-submissions {
  margin-top: 24px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 20px;
}
.leaderboard-submissions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
.leaderboard-submission-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.leaderboard-submission-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  background: #fafafa;
  border-radius: 10px;
}
.submission-month {
  font-size: 13px;
  color: #8c8c8c;
}
.submission-amount {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}
.submission-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.submission-thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
}
.submission-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}
.submission-status.pending { background: #fff7e6; color: #d48806; }
.submission-status.approved { background: #f6ffed; color: #389e0d; }
.submission-status.rejected { background: #fff1f0; color: #cf1322; }
.submission-btn {
  padding: 4px 10px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
}
.submission-btn.pass { background: #f6ffed; color: #389e0d; }
.submission-btn.reject { background: #fff1f0; color: #cf1322; }
```

- [ ] **Step 4: 运行验证**

切换「月度 / 年度」，确认榜单金额与单位变化；当前用户无申报时显示空状态。

- [ ] **Step 5: Commit**

```bash
git add project/user/web/src/views/console/LeaderboardIndex.vue
git commit -m "feat(leaderboard): 自媒体收入榜月度/年度切换与申报记录列表"
```

---

## Task 10: 实现收入申报弹框

**Files:**
- Modify: `project/user/web/src/views/console/LeaderboardIndex.vue`
- Test: 浏览器提交申报、审核、金额累加

**Interfaces:**
- Produces: `openIncomeModal()`, `submitIncome()`

- [ ] **Step 1: 添加申报弹框模板**

```vue
<a-modal
  v-model:open="incomeModalVisible"
  title="申报自媒体收入"
  :footer="null"
  :width="480"
  centered
  class="income-modal"
>
  <div class="income-form">
    <div class="income-form-item">
      <label>申报月份</label>
      <select v-model="incomeForm.month" class="leaderboard-select">
        <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}</option>
      </select>
    </div>
    <div class="income-form-item">
      <label>收入金额（元）</label>
      <input v-model.number="incomeForm.amount" type="number" min="1" step="0.01" class="leaderboard-input" placeholder="请输入收入金额" />
    </div>
    <div class="income-form-item">
      <label>收益截图</label>
      <input type="file" accept="image/jpeg,image/png" @change="handleScreenshotChange" />
      <img v-if="incomeForm.screenshot" :src="incomeForm.screenshot" class="income-preview" />
      <div v-if="screenshotError" class="income-error">{{ screenshotError }}</div>
    </div>
    <div class="income-form-actions">
      <button class="leaderboard-action-btn secondary" @click="incomeModalVisible = false">取消</button>
      <button class="leaderboard-action-btn" :disabled="!canSubmitIncome" @click="submitIncome">提交申报</button>
    </div>
  </div>
</a-modal>
```

- [ ] **Step 2: 在 script 中增加表单逻辑**

```js
const incomeModalVisible = ref(false)
const incomeForm = ref({ month: monthOptions[0], amount: null, screenshot: '' })
const screenshotError = ref('')

const canSubmitIncome = computed(() => {
  return incomeForm.value.month &&
    incomeForm.value.amount > 0 &&
    incomeForm.value.screenshot &&
    !screenshotError.value
})

function openIncomeModal() {
  incomeForm.value = { month: incomeMonth.value || monthOptions[0], amount: null, screenshot: '' }
  screenshotError.value = ''
  incomeModalVisible.value = true
}

function handleScreenshotChange(e) {
  const file = e.target.files[0]
  screenshotError.value = ''
  if (!file) {
    incomeForm.value.screenshot = ''
    return
  }
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    screenshotError.value = '请上传 jpg/png 格式的图片'
    incomeForm.value.screenshot = ''
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    screenshotError.value = '图片大小不能超过 5MB'
    incomeForm.value.screenshot = ''
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    incomeForm.value.screenshot = reader.result
  }
  reader.readAsDataURL(file)
}

function submitIncome() {
  if (!canSubmitIncome.value) return
  submitIncomeSubmission({
    month: incomeForm.value.month,
    amount: incomeForm.value.amount,
    screenshot: incomeForm.value.screenshot
  })
  incomeModalVisible.value = false
  message.success('申报已提交，等待审核')
}
```

- [ ] **Step 3: 添加弹框样式**

```css
.income-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.income-form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.income-form-item label {
  font-size: 13px;
  color: #595959;
}
.leaderboard-input {
  height: 36px;
  padding: 0 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
}
.income-preview {
  max-width: 100%;
  max-height: 160px;
  border-radius: 8px;
  margin-top: 8px;
}
.income-error {
  color: #ff4d4f;
  font-size: 12px;
}
.income-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}
```

- [ ] **Step 4: 运行验证**

1. 点击「申报收入」→ 填写金额 → 选择截图 → 提交。
2. 我的申报记录出现「审核中」。
3. 点击「通过」，金额加入榜单。
4. 再次申报同月份，金额累加。

- [ ] **Step 5: Commit**

```bash
git add project/user/web/src/views/console/LeaderboardIndex.vue
git commit -m "feat(leaderboard): 自媒体收入申报弹框与截图上传"
```

---

## Task 11: 完善规则弹框内容与空状态

**Files:**
- Modify: `project/user/web/src/views/console/LeaderboardIndex.vue`
- Test: 浏览器打开规则弹框、查看空状态

- [ ] **Step 1: 填充规则弹框内容**

```vue
<ol class="leaderboard-rules-list">
  <li><b>创作币榜</b>：按自然月统计用户在平台获得的创作币收益（含风格市场收益、邀请返利等），排名前 10 的用户次月 1 日各获得 <span class="leaderboard-rule-highlight">100 创作币</span>奖励。</li>
  <li><b>自媒体收入榜</b>：支持查看月度榜与年度榜；用户可申报自己在自媒体平台（公众号、小红书、抖音等）的真实收入，填写金额并上传收益截图，审核通过后金额累加生效。</li>
  <li><b>月度奖励</b>：自媒体收入榜每月排名前 10 的用户，次月 1 日各获得 <span class="leaderboard-rule-highlight">100 创作币</span>奖励；年度榜仅做展示，不设奖励。</li>
  <li><b>审核说明</b>：提交后进入审核，审核中金额不计入榜单；如截图不清晰、金额存疑或涉嫌造假，平台有权拒绝申报。</li>
  <li><b>公平性</b>：禁止通过虚假截图、重复申报、机器刷量等违规方式获取排名，一经发现取消当月排名及奖励。</li>
</ol>
<div class="leaderboard-rules-footer">* 活动最终解释权归平台所有。</div>
```

- [ ] **Step 2: 添加空状态**

```vue
<div v-if="coinList.length === 0" class="leaderboard-empty">
  暂无数据，快去创作赚币吧
  <div class="leaderboard-empty-actions">
    <button class="leaderboard-action-btn" @click="$router.push('/console/style-market')">去风格市场</button>
    <button class="leaderboard-action-btn secondary" @click="$router.push('/console/earnings')">查看我的账户</button>
  </div>
</div>
```

- [ ] **Step 3: 添加规则与空状态样式**

```css
.leaderboard-rules-list {
  margin: 0;
  padding-left: 20px;
  font-size: 14px;
  color: #595959;
  line-height: 1.8;
}
.leaderboard-rules-list li {
  margin-bottom: 10px;
}
.leaderboard-rule-highlight {
  color: #ff2442;
  font-weight: 500;
  text-decoration: underline;
  text-underline-offset: 3px;
}
.leaderboard-rules-footer {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #f0f0f0;
  font-size: 13px;
  color: #8c8c8c;
}
.leaderboard-empty {
  text-align: center;
  padding: 60px 0;
  color: #8c8c8c;
}
.leaderboard-empty-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}
```

- [ ] **Step 4: 运行验证**

打开规则弹框确认 5 条规则完整；清空 localStorage 后确认创作币榜空状态出现。

- [ ] **Step 5: Commit**

```bash
git add project/user/web/src/views/console/LeaderboardIndex.vue
git commit -m "feat(leaderboard): 规则弹框内容与榜单空状态"
```

---

## Task 12: 暗色主题适配

**Files:**
- Modify: `project/user/web/src/views/console/LeaderboardIndex.vue`
- Test: 浏览器切换暗色主题

- [ ] **Step 1: 添加暗色主题覆盖样式**

在 `<style scoped>` 底部追加：

```css
body[data-theme="dark"] .leaderboard-title,
body[data-theme="dark"] .leaderboard-submissions-header,
body[data-theme="dark"] .top-nickname,
body[data-theme="dark"] .leaderboard-nickname,
body[data-theme="dark"] .submission-amount {
  color: #f0f0f0;
}
body[data-theme="dark"] .leaderboard-subtitle,
body[data-theme="dark"] .submission-month,
body[data-theme="dark"] .leaderboard-empty,
body[data-theme="dark"] .leaderboard-rules-list,
body[data-theme="dark"] .leaderboard-rules-footer,
body[data-theme="dark"] .income-form-item label {
  color: #a6a6a6;
}
body[data-theme="dark"] .leaderboard-tabs,
body[data-theme="dark"] .leaderboard-period-tabs {
  background: #141414;
}
body[data-theme="dark"] .leaderboard-tab,
body[data-theme="dark"] .leaderboard-period-tab {
  color: #a6a6a6;
}
body[data-theme="dark"] .leaderboard-tab.active,
body[data-theme="dark"] .leaderboard-period-tab.active {
  background: #2a2a2a;
  color: #f0f0f0;
  box-shadow: none;
}
body[data-theme="dark"] .leaderboard-item,
body[data-theme="dark"] .leaderboard-top-card,
body[data-theme="dark"] .leaderboard-submissions,
body[data-theme="dark"] .leaderboard-select,
body[data-theme="dark"] .leaderboard-input {
  background: #1f1f1f;
  border-color: #303030;
  color: #f0f0f0;
}
body[data-theme="dark"] .leaderboard-item.is-me {
  background: rgba(255, 36, 66, 0.12);
  border-color: rgba(255, 36, 66, 0.35);
}
body[data-theme="dark"] .leaderboard-submission-item {
  background: #141414;
}
body[data-theme="dark"] .leaderboard-action-btn.secondary {
  background: #2a2a2a;
  border-color: #ff2442;
}
body[data-theme="dark"] .leaderboard-rules-footer {
  border-color: #303030;
}
body[data-theme="dark"] .income-form-item label {
  color: #a6a6a6;
}
```

- [ ] **Step 2: 运行验证**

点击 Header 昼夜切换按钮，确认榜单、弹框、申报记录在暗色下正常。

- [ ] **Step 3: Commit**

```bash
git add project/user/web/src/views/console/LeaderboardIndex.vue
git commit -m "style(leaderboard): 暗色主题样式适配"
```

---

## Task 13: 可选 - 收益明细类型识别

**Files:**
- Modify: `project/user/web/src/views/console/EarningsIndex.vue`
- Modify: `project/user/web/src/composables/useStyleMarket.js`

- [ ] **Step 1: 在 EarningsIndex.vue 增加 leaderboard_reward 类型显示**

```js
const formatType = (type) => {
  const map = { usage: '使用收益', milestone: '里程碑奖励', leaderboard_reward: '榜单奖励' }
  return map[type] || type
}
```

- [ ] **Step 2: 在 useStyleMarket.js loadEarningsRecords 中兼容新类型**

无需修改，现有 `JSON.parse` 已兼容。

- [ ] **Step 3: 运行验证**

发放奖励后进入「我的账户」→ 收益明细，确认出现「榜单奖励」类型。

- [ ] **Step 4: Commit**

```bash
git add project/user/web/src/views/console/EarningsIndex.vue
git commit -m "feat(leaderboard): 收益明细支持榜单奖励类型"
```

---

## Task 14: 移动端适配

**Files:**
- Modify: `project/user/web/src/views/console/LeaderboardIndex.vue`
- Test: 浏览器 DevTools 切换移动设备

- [ ] **Step 1: 添加响应式样式**

```css
@media (max-width: 768px) {
  .leaderboard-page {
    padding: 20px 16px;
  }
  .leaderboard-top3 {
    grid-template-columns: 1fr;
  }
  .leaderboard-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
  .leaderboard-submission-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  .submission-right {
    width: 100%;
    justify-content: flex-end;
  }
}
```

- [ ] **Step 2: 运行验证**

DevTools 切换 iPhone SE / 375px 宽度，确认无横向滚动、Tab 不溢出。

- [ ] **Step 3: Commit**

```bash
git add project/user/web/src/views/console/LeaderboardIndex.vue
git commit -m "style(leaderboard): 移动端响应式适配"
```

---

## Task 15: 编写 Playwright 验证脚本

**Files:**
- Create: `tests/e2e/verify_leaderboard.py`
- Test: `python3 tests/e2e/verify_leaderboard.py`

- [ ] **Step 1: 创建验证脚本**

```python
from playwright.sync_api import sync_playwright
import sys

BASE_URL = 'http://localhost:5173'

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 800})
        page.goto(f'{BASE_URL}/console/leaderboard')
        page.wait_for_selector('.leaderboard-page', timeout=10000)

        # 1. 页面标题与 Tab
        assert page.locator('.leaderboard-title').inner_text() == '收益排行榜'
        assert page.locator('.leaderboard-tab', has_text='创作币榜').count() == 1
        assert page.locator('.leaderboard-tab', has_text='自媒体收入榜').count() == 1

        # 2. 创作币榜有排名数据
        assert page.locator('.leaderboard-top-card').count() == 3
        assert page.locator('.leaderboard-item').count() >= 1

        # 3. 切换到自媒体收入榜
        page.locator('.leaderboard-tab', has_text='自媒体收入榜').click()
        page.wait_for_timeout(300)
        assert page.locator('button:has-text("申报收入")').count() == 1

        # 4. 打开规则弹框
        page.locator('text=榜单规则').click()
        page.wait_for_selector('.leaderboard-rules-list', timeout=3000)
        assert '创作币榜' in page.locator('.leaderboard-rules-list').inner_text()
        page.locator('.ant-modal-close').click()

        print('All leaderboard checks passed.')
        browser.close()
        sys.exit(0)

if __name__ == '__main__':
    run()
```

- [ ] **Step 2: 运行验证**

```bash
cd project/user/web
npm run dev
# 新终端
python3 tests/e2e/verify_leaderboard.py
```

Expected: `All leaderboard checks passed.`

- [ ] **Step 3: Commit**

```bash
git add tests/e2e/verify_leaderboard.py
git commit -m "test(leaderboard): 新增收益排行榜 Playwright 验证脚本"
```

---

## Task 16: 最终构建与回归检查

**Files:**
- All of the above
- Test: `npm run build`

- [ ] **Step 1: 运行构建**

```bash
cd project/user/web
npm run build
```

Expected: 构建成功，无 TypeScript/Vite 错误。

- [ ] **Step 2: 浏览器最终检查**

1. 侧边栏「收益排行榜」可点击。
2. 创作币榜显示前 3 名卡片与后续列表。
3. 切换月份排名变化。
4. 自媒体收入榜月度/年度切换正常。
5. 申报收入、模拟通过、模拟发放奖励流程正常。
6. 规则弹框完整。
7. 暗色主题正常。

- [ ] **Step 3: Commit（如还有未提交改动）**

```bash
git add -A
git commit -m "feat(leaderboard): 收益排行榜功能完成"
```

---

## Self-Review Checklist

**Spec coverage:**
- [x] 新增独立菜单页 — Task 5
- [x] 创作币榜月度排名 — Task 2, 7
- [x] 自媒体收入榜月度/年度 — Task 3, 9
- [x] 收入申报、截图上传、模拟审核 — Task 3, 10
- [x] 审核通过后累加生效 — Task 3
- [x] 月度前 10 名奖励 100 创作币 — Task 4
- [x] 规则说明弹框 — Task 11
- [x] 暗色主题 — Task 12
- [x] 移动端适配 — Task 14

**Placeholder scan:**
- [x] 无 TBD/TODO
- [x] 所有代码块含完整示例代码
- [x] 无 "适当处理" 等模糊描述

**Type consistency:**
- [x] `getCoinLeaderboard(month)` 与调用处 `coinMonth.value` 一致
- [x] `getIncomeLeaderboard(periodType, periodValue)` 与调用处 `incomePeriodType.value, value` 一致
- [x] `incomeSubmissions` 结构统一使用 `month` 字段
- [x] 奖励记录使用 `leaderboardType + periodValue + userId` 去重

**Gaps:** 无遗漏。

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-02-earnings-leaderboard-plan.md`.

Two execution options:

1. **Subagent-Driven (recommended)** - Dispatch a fresh subagent per task, review between tasks, fast iteration.
2. **Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints.

Which approach?
