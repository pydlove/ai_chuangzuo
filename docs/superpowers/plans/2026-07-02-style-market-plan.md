# 风格市场 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Vue 控制台实现前端 mock 的「风格市场」：用户可分享我的风格 / 学习的风格，经模拟审核后上架市场，他人使用扣 0.2 创作币，创作者实时收益并按周获得里程碑奖励，收益明细独立页面展示。

**Architecture:** 新建 `useStyleMarket.js` 作为市场、收益、结算的共享状态层；新建 `StyleMarketIndex.vue` 和 `EarningsIndex.vue` 两个页面；在 `StylesIndex.vue` 增加分享入口与审核状态；`ConsoleLayout.vue` 导航改为「我的风格」子菜单；`CreateIndex.vue` 支持 `marketStyleId` 参数自动应用市场风格。

**Tech Stack:** Vue 3 Composition API, Vue Router 4, Ant Design Vue, localStorage, Playwright e2e.

## Global Constraints

- 仅在 `project/user/web/` 内实现，不改动 HTML 原型。
- 不引入 npm 包。
- 前端 mock：localStorage 持久化，无后端。
- 创作币余额 key 复用现有 `aichuangzuo_coin_balance`。
- 单次使用价格固定 0.2 创作币。
- 里程碑奖励每周日 23:59 自动结算，只取最高档发放一次。

---

## Task 1: 创建 useStyleMarket.js 共享状态与核心逻辑

**Files:**
- Create: `project/user/web/src/composables/useStyleMarket.js`
- Test: `tests/e2e/verify_style_market.py`（后续任务完整编写，本任务只验证模块导出）

**Interfaces:**
- Produces: `marketStyles`, `earningsRecords`, `shareStyleToMarket(style, sourceType)`, `approveMarketStyle(marketId)`, `useMarketStyle(marketId)`, `settleWeeklyMilestone()`, `simulateExternalUse(marketId)`, `getCoinBalance()`, `getMarketStyleEarnings(marketId)`, `getTotalEarnings()`, `getWeeklyEarnings()`
- Consumes: 无

- [ ] **Step 1: 创建文件骨架与 localStorage 读写**

```js
import { ref } from 'vue'

const MARKET_KEY = 'aichuangzuo_style_market'
const EARNINGS_KEY = 'aichuangzuo_earnings_records'
const COIN_BALANCE_KEY = 'aichuangzuo_coin_balance'
const USER_ID_KEY = 'aichuangzuo_user_id'

const PRICE_PER_USE = 0.2

function loadMarketStyles() {
  try {
    const raw = localStorage.getItem(MARKET_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveMarketStyles() {
  localStorage.setItem(MARKET_KEY, JSON.stringify(marketStyles.value))
}

function loadEarningsRecords() {
  try {
    const raw = localStorage.getItem(EARNINGS_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveEarningsRecords() {
  localStorage.setItem(EARNINGS_KEY, JSON.stringify(earningsRecords.value))
}

function getUserId() {
  let id = localStorage.getItem(USER_ID_KEY)
  if (!id) {
    id = 'u_' + Math.random().toString(36).slice(2, 10)
    localStorage.setItem(USER_ID_KEY, id)
  }
  return id
}

export function getCoinBalance() {
  const raw = localStorage.getItem(COIN_BALANCE_KEY)
  return raw ? parseFloat(raw) : 0
}

function setCoinBalance(balance) {
  localStorage.setItem(COIN_BALANCE_KEY, String(balance))
}

export const marketStyles = ref(loadMarketStyles())
export const earningsRecords = ref(loadEarningsRecords())
```

- [ ] **Step 2: 实现 shareStyleToMarket**

```js
export function shareStyleToMarket(style, sourceType) {
  const existing = marketStyles.value.find(
    s => s.originalName === style.name && s.creatorId === getUserId() && s.sourceType === sourceType
  )
  if (existing) {
    throw new Error('该风格已经分享过')
  }
  const id = 'market-' + Date.now().toString(36)
  marketStyles.value.unshift({
    id,
    name: style.name,
    sourceType,
    originalName: style.name,
    creatorId: getUserId(),
    creatorName: '我',
    prompt: style.prompt,
    scope: style.scope || '',
    excerpt1: style.excerpt1 || '',
    excerpt2: style.excerpt2 || '',
    status: 'pending',
    price: PRICE_PER_USE,
    weeklyUses: 0,
    totalUses: 0,
    weeklyEarnings: 0,
    milestoneBonus: 0,
    lastSettlementAt: new Date().toISOString(),
    createdAt: new Date().toISOString()
  })
  saveMarketStyles()
  return id
}
```

- [ ] **Step 3: 实现 approveMarketStyle**

```js
export function approveMarketStyle(marketId) {
  const s = marketStyles.value.find(x => x.id === marketId)
  if (s) {
    s.status = 'approved'
    saveMarketStyles()
  }
}
```

- [ ] **Step 4: 实现 useMarketStyle 与收益记录**

```js
export function useMarketStyle(marketId) {
  const s = marketStyles.value.find(x => x.id === marketId)
  if (!s) throw new Error('风格不存在')
  if (s.status !== 'approved') throw new Error('风格未上架')

  const balance = getCoinBalance()
  if (balance < PRICE_PER_USE) {
    throw new Error('余额不足')
  }

  // 扣消费者
  setCoinBalance(Number((balance - PRICE_PER_USE).toFixed(2)))
  // 给创作者
  const creatorBalance = getCoinBalance()
  setCoinBalance(Number((creatorBalance + PRICE_PER_USE).toFixed(2)))

  s.weeklyUses += 1
  s.totalUses += 1
  s.weeklyEarnings = Number((s.weeklyUses * PRICE_PER_USE).toFixed(2))
  saveMarketStyles()

  earningsRecords.value.unshift({
    id: 'earn-' + Date.now().toString(36),
    type: 'usage',
    styleName: s.name,
    styleId: s.id,
    amount: PRICE_PER_USE,
    fromUserId: getUserId(),
    description: `使用「${s.name}」生成文章`,
    createdAt: new Date().toISOString()
  })
  saveEarningsRecords()
}
```

注意：单用户 mock 场景下，消费者和创作者都是当前用户，所以余额先扣后加净变化为 0。真实后端替换时此处会区分两个用户。

- [ ] **Step 5: 实现里程碑结算**

```js
function calcMilestoneBonus(weeklyUses) {
  if (weeklyUses >= 1000) return 60
  if (weeklyUses >= 500) return 30
  if (weeklyUses >= 200) return 15
  if (weeklyUses >= 50) return 5
  return 0
}

export function settleWeeklyMilestone() {
  const now = new Date()
  marketStyles.value.forEach(s => {
    if (s.status !== 'approved') return
    const last = new Date(s.lastSettlementAt)
    // 判断是否已经过了一周（简单按 7 天）
    const daysSince = (now.getTime() - last.getTime()) / (1000 * 60 * 60 * 24)
    if (daysSince < 7) return

    const bonus = calcMilestoneBonus(s.weeklyUses)
    if (bonus > 0) {
      const balance = getCoinBalance()
      setCoinBalance(Number((balance + bonus).toFixed(2)))
      s.milestoneBonus = bonus
      earningsRecords.value.unshift({
        id: 'earn-' + Date.now().toString(36) + '-' + s.id,
        type: 'milestone',
        styleName: s.name,
        styleId: s.id,
        amount: bonus,
        description: `「${s.name}」本周使用 ${s.weeklyUses} 次，获得里程碑奖励`,
        createdAt: new Date().toISOString()
      })
      saveEarningsRecords()
    }

    s.weeklyUses = 0
    s.weeklyEarnings = 0
    s.milestoneBonus = 0
    s.lastSettlementAt = now.toISOString()
  })
  saveMarketStyles()
}
```

- [ ] **Step 6: 实现模拟外部使用（单用户演示）**

```js
export function simulateExternalUse(marketId) {
  const s = marketStyles.value.find(x => x.id === marketId)
  if (!s) throw new Error('风格不存在')
  if (s.status !== 'approved') throw new Error('风格未上架')

  const balance = getCoinBalance()
  if (balance < PRICE_PER_USE) {
    throw new Error('余额不足')
  }

  setCoinBalance(Number((balance - PRICE_PER_USE).toFixed(2)))
  // 模拟外部用户支付，创作者收益直接加
  const creatorBalance = getCoinBalance()
  setCoinBalance(Number((creatorBalance + PRICE_PER_USE).toFixed(2)))

  s.weeklyUses += 1
  s.totalUses += 1
  s.weeklyEarnings = Number((s.weeklyUses * PRICE_PER_USE).toFixed(2))
  saveMarketStyles()

  earningsRecords.value.unshift({
    id: 'earn-' + Date.now().toString(36),
    type: 'usage',
    styleName: s.name,
    styleId: s.id,
    amount: PRICE_PER_USE,
    fromUserId: 'external-user',
    description: `其他用户使用「${s.name}」生成文章`,
    createdAt: new Date().toISOString()
  })
  saveEarningsRecords()
}
```

- [ ] **Step 7: 实现收益统计函数**

```js
export function getMarketStyleEarnings(marketId) {
  return earningsRecords.value
    .filter(r => r.styleId === marketId && r.amount > 0)
    .reduce((sum, r) => sum + r.amount, 0)
}

export function getTotalEarnings() {
  return earningsRecords.value
    .filter(r => r.amount > 0)
    .reduce((sum, r) => sum + r.amount, 0)
}

export function getWeeklyEarnings() {
  const weekAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString()
  return earningsRecords.value
    .filter(r => r.createdAt > weekAgo && r.amount > 0)
    .reduce((sum, r) => sum + r.amount, 0)
}
```

- [ ] **Step 8: 运行简单验证**

在浏览器控制台或临时脚本中验证：

```js
localStorage.clear()
import('/src/composables/useStyleMarket.js').then(m => {
  const id = m.shareStyleToMarket({ name: '测试风格', prompt: '...', scope: '测试' }, 'my')
  m.approveMarketStyle(id)
  m.useMarketStyle(id)
  console.log(m.marketStyles.value)
  console.log(m.earningsRecords.value)
})
```

Expected: marketStyles 有一条 approved 记录，weeklyUses=1，earningsRecords 有一条 usage 记录。

- [ ] **Step 9: Commit**

```bash
git add project/user/web/src/composables/useStyleMarket.js
git commit -m "feat(style-market): 添加市场状态与计费结算逻辑

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 2: StylesIndex.vue 增加分享入口与审核状态

**Files:**
- Modify: `project/user/web/src/views/console/StylesIndex.vue`
- Test: `tests/e2e/verify_style_market.py`

**Interfaces:**
- Consumes: `shareStyleToMarket(style, sourceType)`, `approveMarketStyle(marketId)`, `marketStyles`
- Produces: 卡片上「分享」「模拟通过」按钮，审核状态标签

- [ ] **Step 1: 导入 useStyleMarket**

```js
import {
  marketStyles,
  shareStyleToMarket,
  approveMarketStyle
} from '@/composables/useStyleMarket.js'
```

- [ ] **Step 2: 我的风格卡片增加分享按钮与状态标签**

在「我的风格」卡片模板中，在 `style-card-actions` 前增加状态行，在 actions 中增加「分享」按钮：

```vue
<div class="style-card-title">{{ s.name }}</div>
<div class="style-card-desc">{{ s.desc }} · 已用 {{ s.count }} 次</div>
<div v-if="s.scope" class="style-card-scope">适用：{{ s.scope }}</div>
<div v-if="getMarketStatus(s.name)" class="style-card-market-tag">
  {{ getMarketStatus(s.name) }}
</div>
<div class="style-card-prompt">{{ promptSummary(s.prompt) }}</div>
<div v-show="expandedNames.has(s.name)" class="style-prompt-full">{{ s.prompt }}</div>
<div class="style-card-actions">
  <button class="style-action-btn" @click.stop="useStyle(s)">使用</button>
  <button class="style-action-btn" @click.stop="togglePrompt(s.name)">
    {{ expandedNames.has(s.name) ? '收起' : '查看完整提示词' }}
  </button>
  <button class="style-action-btn" @click.stop="goToEdit(s)">编辑</button>
  <button
    v-if="getMarketStatus(s.name) === '审核中'"
    class="style-action-btn"
    @click.stop="simulateApprove(s.name)"
  >模拟通过</button>
  <button
    v-else-if="!getMarketStatus(s.name)"
    class="style-action-btn"
    @click.stop="shareStyle(s, 'my')"
  >分享</button>
  <button class="style-action-btn style-del-btn" @click.stop="deleteStyle(s.name)">删除</button>
</div>
```

- [ ] **Step 3: 学习的风格卡片同样增加分享入口**

在「学习的风格」卡片中，在 `style-card-source` 后增加状态标签，在 actions 中增加「分享」「模拟通过」按钮（逻辑同上，sourceType 传 'learned'）。

- [ ] **Step 4: 添加辅助函数**

```js
const getMarketStatus = (name) => {
  const s = marketStyles.value.find(
    m => m.originalName === name && m.creatorId === localStorage.getItem('aichuangzuo_user_id')
  )
  if (!s) return ''
  if (s.status === 'pending') return '审核中'
  if (s.status === 'approved') return '已上架'
  return ''
}

const shareStyle = (style, sourceType) => {
  try {
    shareStyleToMarket(style, sourceType)
  } catch (err) {
    alert(err.message)
  }
}

const simulateApprove = (name) => {
  const s = marketStyles.value.find(
    m => m.originalName === name && m.creatorId === localStorage.getItem('aichuangzuo_user_id')
  )
  if (s) approveMarketStyle(s.id)
}
```

- [ ] **Step 5: 添加样式**

```css
.style-card-market-tag {
  display: inline-block;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 10px;
  background: #fff0f2;
  color: #ff2442;
  border: 1px solid #ffd1d9;
}
```

- [ ] **Step 6: 启动服务并手动验证**

```bash
./scripts/local/start.sh
```

访问 `http://localhost:28585/.superpowers/brainstorm/...` 不是正确地址；Vue 控制台运行在 `npm run dev` 的端口。请运行：

```bash
cd project/user/web && npm run dev
```

打开 `http://localhost:5173/console/styles`（或实际端口），创建我的风格，点击「分享」，确认出现「审核中」，点击「模拟通过」后变为「已上架」。

- [ ] **Step 7: Commit**

```bash
git add project/user/web/src/views/console/StylesIndex.vue
git commit -m "feat(style-market): 我的风格与学习风格卡片增加分享与审核状态

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 3: 创建 StyleMarketIndex.vue 市场发现页

**Files:**
- Create: `project/user/web/src/views/console/StyleMarketIndex.vue`
- Test: `tests/e2e/verify_style_market.py`

**Interfaces:**
- Consumes: `marketStyles`, `useMarketStyle(marketId)`, `simulateExternalUse(marketId)`, `getCoinBalance()`（来自 useStyleMarket 或 useInviteCode）
- Produces: `/console/style-market` 页面

- [ ] **Step 1: 创建页面文件**

```vue
<template>
  <div class="style-market-index">
    <div class="style-market-header">
      <h2 class="style-market-title">风格市场</h2>
      <p class="style-market-subtitle">发现优质写作风格，支持原创创作者</p>
    </div>

    <div class="style-market-stats">
      <div class="style-market-stat">
        <div class="style-market-stat-value">{{ approvedStyles.length }}</div>
        <div class="style-market-stat-label">上架风格</div>
      </div>
      <div class="style-market-stat">
        <div class="style-market-stat-value">{{ totalWeeklyUses }}</div>
        <div class="style-market-stat-label">本周使用</div>
      </div>
      <div class="style-market-stat">
        <div class="style-market-stat-value">{{ coinBalance }}</div>
        <div class="style-market-stat-label">我的余额</div>
      </div>
    </div>

    <div class="style-market-search">
      <input
        v-model="searchQuery"
        type="text"
        class="style-market-search-input"
        placeholder="搜索风格名或适用范围"
      />
    </div>

    <div v-if="filteredStyles.length === 0" class="style-market-empty">
      暂无已上架风格
    </div>
    <div v-else class="style-market-grid">
      <div
        v-for="s in filteredStyles"
        :key="s.id"
        class="style-market-card"
      >
        <div class="style-market-card-title">{{ s.name }}</div>
        <div class="style-market-card-creator">by {{ s.creatorName }}</div>
        <div v-if="s.scope" class="style-market-card-scope">适用：{{ s.scope }}</div>
        <div class="style-market-card-prompt">{{ promptSummary(s.prompt) }}</div>
        <div class="style-market-card-stats">
          <span>本周 {{ s.weeklyUses }} 次</span>
          <span>累计 {{ s.totalUses }} 次</span>
        </div>
        <div class="style-market-card-actions">
          <button
            class="style-market-use-btn"
            :disabled="coinBalance < s.price"
            @click="handleUse(s)"
          >
            使用（{{ s.price }} 币）
          </button>
          <button
            v-if="s.creatorId === currentUserId"
            class="style-market-simulate-btn"
            @click="handleSimulate(s)"
          >
            模拟他人使用
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  marketStyles,
  useMarketStyle,
  simulateExternalUse,
  getCoinBalance
} from '@/composables/useStyleMarket.js'

const router = useRouter()
const searchQuery = ref('')
const currentUserId = ref(localStorage.getItem('aichuangzuo_user_id') || '')
const coinBalance = ref(getCoinBalance())

const approvedStyles = computed(() =>
  marketStyles.value.filter(s => s.status === 'approved')
)

const totalWeeklyUses = computed(() =>
  approvedStyles.value.reduce((sum, s) => sum + s.weeklyUses, 0)
)

const filteredStyles = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return approvedStyles.value
  return approvedStyles.value.filter(
    s =>
      s.name.toLowerCase().includes(q) ||
      (s.scope && s.scope.toLowerCase().includes(q))
  )
})

const promptSummary = (prompt) => {
  if (!prompt) return ''
  return prompt.length > 60 ? prompt.slice(0, 60) + '...' : prompt
}

const refreshBalance = () => {
  coinBalance.value = getCoinBalance()
}

const handleUse = (s) => {
  try {
    useMarketStyle(s.id)
    refreshBalance()
    router.push(`/console/create?marketStyleId=${s.id}`)
  } catch (err) {
    alert(err.message)
  }
}

const handleSimulate = (s) => {
  try {
    simulateExternalUse(s.id)
    refreshBalance()
  } catch (err) {
    alert(err.message)
  }
}
</script>

<style scoped>
.style-market-index {
  height: 100%;
  padding: 24px;
  overflow-y: auto;
}

.style-market-header {
  margin-bottom: 20px;
}

.style-market-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.style-market-subtitle {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.style-market-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.style-market-stat {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px 24px;
  min-width: 120px;
}

.style-market-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #ff2442;
}

.style-market-stat-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}

.style-market-search {
  margin-bottom: 20px;
}

.style-market-search-input {
  width: 100%;
  max-width: 400px;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  font-size: 14px;
}

.style-market-search-input:focus {
  outline: none;
  border-color: #ff2442;
}

.style-market-empty {
  padding: 60px 0;
  text-align: center;
  color: #8c8c8c;
}

.style-market-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.style-market-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.style-market-card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.style-market-card-creator {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.style-market-card-scope {
  font-size: 12px;
  color: #ff2442;
  background: #fff0f2;
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.style-market-card-prompt {
  font-size: 12px;
  color: #595959;
  line-height: 1.5;
  margin-bottom: 12px;
  flex: 1;
  white-space: pre-line;
}

.style-market-card-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 12px;
}

.style-market-card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.style-market-use-btn {
  flex: 1;
  padding: 8px 16px;
  background: #ff2442;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

.style-market-use-btn:hover {
  background: #e61e3a;
}

.style-market-use-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

.style-market-simulate-btn {
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  font-size: 13px;
  color: #595959;
  cursor: pointer;
}

.style-market-simulate-btn:hover {
  border-color: #ff2442;
  color: #ff2442;
}
</style>
```

- [ ] **Step 2: 启动服务并验证页面可访问**

先完成 Task 4 路由添加后再一起验证。

- [ ] **Step 3: Commit**

```bash
git add project/user/web/src/views/console/StyleMarketIndex.vue
git commit -m "feat(style-market): 创建风格市场发现页

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 4: 创建 EarningsIndex.vue 收益明细页

**Files:**
- Create: `project/user/web/src/views/console/EarningsIndex.vue`
- Test: `tests/e2e/verify_style_market.py`

**Interfaces:**
- Consumes: `earningsRecords`, `getTotalEarnings()`, `getWeeklyEarnings()`
- Produces: `/console/earnings` 页面

- [ ] **Step 1: 创建页面文件**

```vue
<template>
  <div class="earnings-index">
    <div class="earnings-header">
      <h2 class="earnings-title">收益明细</h2>
      <p class="earnings-subtitle">查看风格市场带来的创作币收益</p>
    </div>

    <div class="earnings-stats">
      <div class="earnings-stat">
        <div class="earnings-stat-value">{{ totalEarnings.toFixed(2) }}</div>
        <div class="earnings-stat-label">累计收益</div>
      </div>
      <div class="earnings-stat">
        <div class="earnings-stat-value">{{ weeklyEarnings.toFixed(2) }}</div>
        <div class="earnings-stat-label">本周收益</div>
      </div>
      <div class="earnings-stat">
        <div class="earnings-stat-value">{{ recordCount }}</div>
        <div class="earnings-stat-label">收益笔数</div>
      </div>
    </div>

    <div v-if="earningsRecords.length === 0" class="earnings-empty">
      暂无收益记录
    </div>
    <div v-else class="earnings-list">
      <div
        v-for="r in earningsRecords"
        :key="r.id"
        class="earnings-item"
      >
        <div class="earnings-item-left">
          <div class="earnings-item-title">{{ r.description }}</div>
          <div class="earnings-item-meta">
            {{ r.styleName }} · {{ formatType(r.type) }} · {{ r.createdAt.slice(0, 16).replace('T', ' ') }}
          </div>
        </div>
        <div class="earnings-item-amount" :class="{ negative: r.amount < 0 }">
          {{ r.amount > 0 ? '+' : '' }}{{ r.amount.toFixed(2) }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  earningsRecords,
  getTotalEarnings,
  getWeeklyEarnings
} from '@/composables/useStyleMarket.js'

const totalEarnings = computed(() => getTotalEarnings())
const weeklyEarnings = computed(() => getWeeklyEarnings())
const recordCount = computed(() => earningsRecords.value.length)

const formatType = (type) => {
  const map = { usage: '使用收益', milestone: '里程碑奖励' }
  return map[type] || type
}
</script>

<style scoped>
.earnings-index {
  height: 100%;
  padding: 24px;
  overflow-y: auto;
}

.earnings-header {
  margin-bottom: 20px;
}

.earnings-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.earnings-subtitle {
  font-size: 13px;
  color: #8c8c8c;
  margin: 0;
}

.earnings-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.earnings-stat {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 16px 24px;
  min-width: 120px;
}

.earnings-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #ff2442;
}

.earnings-stat-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}

.earnings-empty {
  padding: 60px 0;
  text-align: center;
  color: #8c8c8c;
}

.earnings-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.earnings-item {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 14px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.earnings-item-title {
  font-size: 14px;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.earnings-item-meta {
  font-size: 12px;
  color: #8c8c8c;
}

.earnings-item-amount {
  font-size: 15px;
  font-weight: 600;
  color: #ff2442;
}

.earnings-item-amount.negative {
  color: #ff4d4f;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add project/user/web/src/views/console/EarningsIndex.vue
git commit -m "feat(style-market): 创建收益明细页

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 5: 更新路由与导航

**Files:**
- Modify: `project/user/web/src/router/index.js`
- Modify: `project/user/web/src/views/console/ConsoleLayout.vue`
- Test: `tests/e2e/verify_style_market.py`

**Interfaces:**
- Produces: `/console/style-market` 与 `/console/earnings` 路由；侧边栏子菜单

- [ ] **Step 1: router/index.js 新增路由**

```js
{
  path: 'styles',
  name: 'ConsoleStyles',
  component: () => import('@/views/console/StylesIndex.vue')
},
{
  path: 'style-market',
  name: 'ConsoleStyleMarket',
  component: () => import('@/views/console/StyleMarketIndex.vue')
},
{
  path: 'earnings',
  name: 'ConsoleEarnings',
  component: () => import('@/views/console/EarningsIndex.vue')
},
```

- [ ] **Step 2: ConsoleLayout.vue 导航改为子菜单**

当前 `navItems` 是扁平数组，需要改为支持子菜单。先修改数据结构：

```js
const navItems = [
  { path: '/console/create', label: '创作', icon: EditOutlined },
  { path: '/console/works', label: '我的作品', icon: FolderOutlined },
  {
    label: '我的风格',
    icon: SmileOutlined,
    children: [
      { path: '/console/styles', label: '我的风格' },
      { path: '/console/style-market', label: '风格市场' },
      { path: '/console/earnings', label: '收益明细' }
    ]
  },
  { path: '/console/hot-search', label: '热搜榜', icon: FireOutlined }
]
```

修改模板渲染：

```vue
<nav class="console-sidebar-nav">
  <template v-for="item in navItems" :key="item.path || item.label">
    <div v-if="item.children" class="console-sidebar-group">
      <div class="console-sidebar-group-title">
        <component :is="item.icon" class="nav-icon" />
        <span>{{ item.label }}</span>
      </div>
      <router-link
        v-for="sub in item.children"
        :key="sub.path"
        :to="sub.path"
        class="console-sidebar-item sub-item"
        :class="{ active: isActive(sub.path) }"
      >
        <span class="nav-label">{{ sub.label }}</span>
      </router-link>
    </div>
    <router-link
      v-else
      :to="item.path"
      class="console-sidebar-item"
      :class="{ active: isActive(item.path) }"
    >
      <component :is="item.icon" class="nav-icon" />
      <span class="nav-label">{{ item.label }}</span>
    </router-link>
  </template>
</nav>
```

增加样式：

```css
.console-sidebar-group {
  margin-bottom: 4px;
}

.console-sidebar-group-title {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  font-size: 14px;
  color: #595959;
  font-weight: 500;
}

.console-sidebar-item.sub-item {
  padding-left: 44px;
  font-size: 13px;
}
```

- [ ] **Step 3: 启动服务验证导航与路由**

```bash
cd project/user/web && npm run dev
```

访问 `http://localhost:5173/console/style-market` 和 `/console/earnings`，确认页面渲染，侧边栏子菜单高亮正常。

- [ ] **Step 4: Commit**

```bash
git add project/user/web/src/router/index.js project/user/web/src/views/console/ConsoleLayout.vue
git commit -m "feat(style-market): 新增市场与收益路由及侧边栏子菜单

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 6: CreateIndex.vue 支持 marketStyleId 参数

**Files:**
- Modify: `project/user/web/src/views/console/CreateIndex.vue`
- Test: `tests/e2e/verify_style_market.py`

**Interfaces:**
- Consumes: `marketStyles`, `applyStyleShared(s)`（即 `applyStyle`）
- Produces: 市场页跳转后自动应用风格

- [ ] **Step 1: 导入 marketStyles 与 applyStyle**

```js
import {
  systemStyles,
  myStyles,
  applyStyle as applyStyleShared,
  learnedStyles
} from '@/composables/useStyles.js'
import { marketStyles } from '@/composables/useStyleMarket.js'
```

- [ ] **Step 2: 页面加载时读取 query 参数**

在 `onMounted` 或 `watch` 中处理。假设已有 `onMounted`，添加：

```js
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

onMounted(() => {
  const marketStyleId = route.query.marketStyleId
  if (marketStyleId) {
    const s = marketStyles.value.find(x => x.id === marketStyleId)
    if (s) {
      applyStyleShared({
        name: s.name,
        prompt: s.prompt,
        scope: s.scope
      })
      selectedStyleName.value = s.name
      // 可选：清除 query 避免刷新重复应用
      router.replace({ path: route.path })
    }
  }
})
```

注意：如果 `CreateIndex.vue` 中已有 `onMounted`，请合并到现有逻辑中，不要重复声明。

- [ ] **Step 3: 验证联动**

在市场页点击「使用」按钮，确认跳转 `/console/create?marketStyleId=xxx`，且创作页当前风格已更新。

- [ ] **Step 4: Commit**

```bash
git add project/user/web/src/views/console/CreateIndex.vue
git commit -m "feat(style-market): 创作页支持 marketStyleId 参数自动应用市场风格

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 7: 端到端测试

**Files:**
- Create: `tests/e2e/verify_style_market.py`

**Interfaces:**
- Consumes: 前面所有任务完成后的 UI
- Produces: 通过的风格市场 e2e 测试脚本

- [ ] **Step 1: 创建测试脚本**

```python
# tests/e2e/verify_style_market.py
import os
from playwright.sync_api import sync_playwright

URL = os.environ.get('APP_URL', 'http://localhost:22345')
SCREENSHOT_DIR = 'tests/e2e/screenshots'
os.makedirs(SCREENSHOT_DIR, exist_ok=True)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 800})

        # 1. 清空市场相关 localStorage
        page.goto(f'{URL}/console/styles')
        page.wait_for_timeout(800)
        page.evaluate("""
          () => {
            localStorage.removeItem('aichuangzuo_style_market')
            localStorage.removeItem('aichuangzuo_earnings_records')
            localStorage.setItem('aichuangzuo_coin_balance', '10')
          }
        """)
        page.reload()
        page.wait_for_timeout(500)

        # 2. 创建我的风格
        page.locator('button:has-text("去创建一个")').click()
        page.wait_for_timeout(300)
        inputs = page.locator('.style-editor-input')
        inputs.nth(0).fill('市场测试风格')
        page.locator('.style-editor-textarea').fill('这是一段用于市场测试的风格提示词。')
        inputs.nth(1).fill('公众号情感文')
        page.locator('.style-editor-form button:has-text("保存")').click()
        page.wait_for_timeout(300)

        # 3. 分享
        page.locator('.styles-content:visible .style-card').filter(
            has_text='市场测试风格'
        ).locator('button:has-text("分享")').click()
        page.wait_for_timeout(300)
        page.locator('.styles-content:visible .style-card').filter(
            has_text='市场测试风格'
        ).locator('button:has-text("模拟通过")').click()
        page.wait_for_timeout(300)

        # 4. 进入市场页
        page.goto(f'{URL}/console/style-market')
        page.wait_for_timeout(800)
        assert '市场测试风格' in page.content()
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_list.png')

        # 5. 使用市场风格
        page.locator('button:has-text("使用（0.2 币）")').first.click()
        page.wait_for_timeout(800)
        assert '/console/create' in page.url
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_applied.png')

        # 6. 进入收益页
        page.goto(f'{URL}/console/earnings')
        page.wait_for_timeout(800)
        assert '使用「市场测试风格」生成文章' in page.content()
        page.screenshot(path=f'{SCREENSHOT_DIR}/style_market_earnings.png')

        print('风格市场验证通过')
        browser.close()


if __name__ == '__main__':
    main()
```

注意：Playwright Python 的 `filter(has_text=...)` 写法需要确认可用；如果不可用，改用 XPath 或更宽松的 selector。

- [ ] **Step 2: 运行测试**

```bash
python3 tests/e2e/verify_style_market.py
```

Expected: 输出「风格市场验证通过」。

- [ ] **Step 3: Commit**

```bash
git add tests/e2e/verify_style_market.py
git commit -m "test(e2e): 添加风格市场端到端验证

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Spec Coverage Self-Check

| 需求 | 对应任务 |
|---|---|
| 我的风格 / 学习的风格可分享 | Task 2 |
| 分享后待审核、模拟通过 | Task 1 + Task 2 |
| 风格市场发现页 | Task 3 |
| 每次使用扣 0.2 币、创作者 +0.2 币 | Task 1 + Task 3 |
| 里程碑奖励每周结算 | Task 1 |
| 收益明细页 | Task 4 |
| 我的风格子菜单导航 | Task 5 |
| 创作页 marketStyleId 联动 | Task 6 |
| e2e 验证 | Task 7 |

## Placeholder Scan

- 无 TBD / TODO。
- 所有代码片段完整。
- 测试脚本包含具体 selector 与断言。

## Type Consistency

- `marketStyles` 数组元素字段与 `shareStyleToMarket` 返回值一致。
- `earningsRecords` 类型 `usage | milestone` 在 `EarningsIndex.vue` 与 `useStyleMarket.js` 中一致。
- `marketStyleId` query 参数在 `StyleMarketIndex.vue` 跳转与 `CreateIndex.vue` 读取中一致。

---

**Execution Handoff**

Plan complete and saved to `docs/superpowers/plans/2026-07-02-style-market-plan.md`. Two execution options:

1. **Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration.
2. **Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints.

Which approach?
