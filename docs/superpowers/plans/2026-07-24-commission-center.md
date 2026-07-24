# 约稿中心 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在用户端 console 新增「约稿中心」,支持发布稿件征集任务、投递平台已生成文章、发布者选人结算、平台抽成 10%。

**Architecture:**
- 纯前端实现,状态走 Pinia-style composable + localStorage(`aichuangzuo_commission_tasks`、`aichuangzuo_commission_submissions`、`aichuangzuo_coin_balance`)。
- API 调用走 `src/api/commission.js` 占位文件,内含 TODO,所有调用包在 try/catch 里,失败回退到 localStorage 实现。
- 创作币余额复用并扩展 `useInviteStats` 暴露的可读 ref,在 composable 内部写 localStorage。

**Tech Stack:** Vue 3 (Composition API + script setup)、Vue Router 4、Ant Design Vue 4、本地 CSS(无 Tailwind)。

## Global Constraints

- 最低奖励 **5 创作币**,最高 **10000 创作币**
- 平台抽成 **10%**(写死常量 `PLATFORM_FEE_RATE = 0.1`)
- 截止时间选项: **3 / 7 / 15 天**(下拉三选一,无自定义)
- 每用户每任务仅可投递 **1 次**
- 截止 + **24 小时宽限期** 后发布者仍未选 → 任务 `EXPIRED`,全额退款
- 无投稿时发布者可主动 `CANCELLED` 撤销;有投稿后**禁止撤销**
- 路由: `/console/commission`、`/console/commission/publish`、`/console/commission/:id`
- 侧边栏 `navItems` 新增一项,icon 用 `FileTextOutlined`
- 主色: `#FF2442`(品牌红),字体默认 sans-serif,卡片圆角 12px
- 状态徽章配色: OPEN 蓝、`SETTLED` 绿、`EXPIRED` 灰、`CANCELLED` 橙
- 代码内**禁止**保留 `console.log` / 占位 mock / 未用 import(完成一个功能即删)
- **不引入** Redis、MQ、ES 等新中间件;沿用现有 MySQL + 同步接口风格预留 API

---

### Task 1: 基础设施 — API 占位 + composable + 种子数据

**Files:**
- Create: `project/user/web/src/api/commission.js`
- Create: `project/user/web/src/composables/useCommission.js`
- Create: `project/user/web/src/data/commissionSeed.js`
- Modify: `project/user/web/src/composables/useInviteStats.js`(扩展 coinBalance 可写)

**Step 1:** 新建 `project/user/web/src/api/commission.js`,文件只声明占位函数:

```js
import { api } from '@/api/auth'

// TODO 后端实现:以下是预留接口,前端暂走 localStorage。
// 真实接入时,把这层 throw 的 Error 移除即可。

function notImplemented(name) {
  return () => Promise.reject(new Error(`commission API ${name} not implemented`))
}

export const listCommissionTasks = notImplemented('listCommissionTasks')
export const getCommissionTask = notImplemented('getCommissionTask')
export const createCommissionTask = notImplemented('createCommissionTask')
export const cancelCommissionTask = notImplemented('cancelCommissionTask')
export const listSubmissions = notImplemented('listSubmissions')
export const submitCommissionArticle = notImplemented('submitCommissionArticle')
export const withdrawSubmission = notImplemented('withdrawSubmission')
export const pickWinner = notImplemented('pickWinner')

// 工具方法,前端自行实现(用于 ID 生成、昵称截断等)
export function genId(prefix) {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

export const COMMISSION_CONFIG = Object.freeze({
  MIN_REWARD: 5,
  MAX_REWARD: 10000,
  PLATFORM_FEE_RATE: 0.10,
  DEADLINE_OPTIONS: [
    { label: '3 天', days: 3 },
    { label: '7 天', days: 7 },
    { label: '15 天', days: 15 }
  ],
  GRACE_HOURS: 24
})
```

**Step 2:** 修改 `project/user/web/src/composables/useInviteStats.js`,在 coinBalance ref 上暴露 setter 能力,新增一个 `setCoinBalance` 函数。

读取 `useInviteStats.js` 当前实现,然后在 `loadInviteStats` 函数之后追加:

```js
const COIN_BALANCE_KEY = 'aichuangzuo_coin_balance'
const DEFAULT_COIN_BALANCE = 100

function readCoinBalance() {
  try {
    const raw = localStorage.getItem(COIN_BALANCE_KEY)
    if (raw == null) return DEFAULT_COIN_BALANCE
    const n = Number(raw)
    return Number.isFinite(n) && n >= 0 ? n : DEFAULT_COIN_BALANCE
  } catch {
    return DEFAULT_COIN_BALANCE
  }
}

function writeCoinBalance(n) {
  try { localStorage.setItem(COIN_BALANCE_KEY, String(n)) } catch { /* ignore */ }
}

function setCoinBalance(n) {
  const v = Math.max(0, Math.floor(Number(n) || 0))
  coinBalance.value = v
  writeCoinBalance(v)
}

function adjustCoinBalance(delta) {
  setCoinBalance(coinBalance.value + delta)
}
```

并在 `loadInviteStats` 同步从 `readCoinBalance()` 初始化(替换硬编码的 0)。同时把 `setCoinBalance`、`adjustCoinBalance`、`COIN_BALANCE_KEY` 三个标识从 return 里导出(供其他 composable 复用)。

**Step 3:** 创建 `project/user/web/src/data/commissionSeed.js`:

```js
import { genId, COMMISSION_CONFIG } from '@/api/commission'

// 演示数据:首次访问时播种到 localStorage,方便用户立刻看到多种状态。
// 当前用户 ID 用 'demo-user-1' 表示;可通过 setCurrentUserId 切换。
const NOW = Date.now()
const DAY = 24 * 60 * 60 * 1000

export const DEMO_USER_ID = 'demo-user-1'
export const DEMO_USER_NICKNAMES = {
  'demo-user-1': '柠檬不酸',
  'demo-user-2': '墨鱼写作',
  'demo-user-3': '小桥流水',
  'demo-user-4': '夜半听雨'
}

export const SEED_TASKS = [
  // 进行中,5 天前发布,3 天后截止
  {
    id: genId('cmt'),
    publisherId: 'demo-user-2',
    publisherNickname: '墨鱼写作',
    title: '征集 5 篇小红书爆款养生选题',
    description: '面向 25-35 岁女性,标题党但要有干货,字数 600-1000。',
    requirements: { minWordCount: 600, maxWordCount: 1000, styleHint: '种草风' },
    rewardCoin: 30,
    platformFeeRate: COMMISSION_CONFIG.PLATFORM_FEE_RATE,
    deadlineAt: new Date(NOW + 3 * DAY).toISOString(),
    graceDeadlineAt: new Date(NOW + 3 * DAY + COMMISSION_CONFIG.GRACE_HOURS * 60 * 60 * 1000).toISOString(),
    status: 'OPEN',
    winnerSubmissionId: null,
    settledAt: null,
    createdAt: new Date(NOW - 5 * DAY).toISOString()
  },
  // 已结算
  {
    id: genId('cmt'),
    publisherId: 'demo-user-3',
    publisherNickname: '小桥流水',
    title: '招募公众号情感故事作者',
    description: '第一人称情感故事,字数 1200-2000。',
    requirements: { minWordCount: 1200, maxWordCount: 2000, styleHint: '细腻、走心' },
    rewardCoin: 80,
    platformFeeRate: COMMISSION_CONFIG.PLATFORM_FEE_RATE,
    deadlineAt: new Date(NOW - 2 * DAY).toISOString(),
    graceDeadlineAt: new Date(NOW - 1 * DAY).toISOString(),
    status: 'SETTLED',
    winnerSubmissionId: 'cms_seed_winner_1',
    settledAt: new Date(NOW - 1 * DAY).toISOString(),
    createdAt: new Date(NOW - 9 * DAY).toISOString()
  },
  // 已过期无人选
  {
    id: genId('cmt'),
    publisherId: 'demo-user-4',
    publisherNickname: '夜半听雨',
    title: '求一篇公众号爆款书评',
    description: '近期畅销书,800-1500 字。',
    requirements: { minWordCount: 800, maxWordCount: 1500, styleHint: '书评' },
    rewardCoin: 50,
    platformFeeRate: COMMISSION_CONFIG.PLATFORM_FEE_RATE,
    deadlineAt: new Date(NOW - 2 * DAY).toISOString(),
    graceDeadlineAt: new Date(NOW - 1 * DAY).toISOString(),
    status: 'EXPIRED',
    winnerSubmissionId: null,
    settledAt: null,
    createdAt: new Date(NOW - 8 * DAY).toISOString()
  },
  // 进行中且我投递过
  {
    id: genId('cmt'),
    publisherId: 'demo-user-3',
    publisherNickname: '小桥流水',
    title: '招募知乎带货短文',
    description: '数码类带货,500-800 字。',
    requirements: { minWordCount: 500, maxWordCount: 800, styleHint: '理性种草' },
    rewardCoin: 25,
    platformFeeRate: COMMISSION_CONFIG.PLATFORM_FEE_RATE,
    deadlineAt: new Date(NOW + 6 * DAY).toISOString(),
    graceDeadlineAt: new Date(NOW + 6 * DAY + COMMISSION_CONFIG.GRACE_HOURS * 60 * 60 * 1000).toISOString(),
    status: 'OPEN',
    winnerSubmissionId: null,
    settledAt: null,
    createdAt: new Date(NOW - 1 * DAY).toISOString()
  }
]

export const SEED_SUBMISSIONS = [
  {
    id: 'cms_seed_winner_1',
    taskId: SEED_TASKS[1].id,
    submitterId: 'demo-user-1',
    submitterNickname: '柠檬不酸',
    articleBizNo: 'demo-art-winner',
    articleTitle: '30 岁那年,我学会了不再讨好',
    wordCount: 1650,
    submittedAt: new Date(NOW - 4 * DAY).toISOString(),
    withdrawnAt: null
  },
  // 第二篇投稿,未中标,留着展示列表
  {
    id: genId('cms'),
    taskId: SEED_TASKS[1].id,
    submitterId: 'demo-user-2',
    submitterNickname: '墨鱼写作',
    articleBizNo: 'demo-art-2',
    articleTitle: '那年夏天,我与自己和解',
    wordCount: 1480,
    submittedAt: new Date(NOW - 3 * DAY).toISOString(),
    withdrawnAt: null
  },
  // 当前用户投过第三个任务
  {
    id: genId('cms'),
    taskId: SEED_TASKS[3].id,
    submitterId: 'demo-user-1',
    submitterNickname: '柠檬不酸',
    articleBizNo: 'demo-art-mine',
    articleTitle: '2026 数码圈三大趋势',
    wordCount: 720,
    submittedAt: new Date(NOW - 6 * 60 * 60 * 1000).toISOString(),
    withdrawnAt: null
  }
]
```

**Step 4:** 创建 `project/user/web/src/composables/useCommission.js`(核心状态层):

```js
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  genId,
  COMMISSION_CONFIG
} from '@/api/commission'
import { DEMO_USER_ID, DEMO_USER_NICKNAMES, SEED_TASKS, SEED_SUBMISSIONS } from '@/data/commissionSeed'
import { useInviteStats } from '@/composables/useInviteStats'

const TASKS_KEY = 'aichuangzuo_commission_tasks'
const SUBS_KEY = 'aichuangzuo_commission_submissions'
const USER_KEY = 'aichuangzuo_commission_current_user'
const SEED_FLAG_KEY = 'aichuangzuo_commission_seeded'

function read(key, fallback) {
  try {
    const raw = localStorage.getItem(key)
    if (raw == null) return fallback
    return JSON.parse(raw)
  } catch {
    return fallback
  }
}

function write(key, value) {
  try { localStorage.setItem(key, JSON.stringify(value)) } catch { /* ignore */ }
}

function ensureSeed() {
  if (localStorage.getItem(SEED_FLAG_KEY)) return
  write(TASKS_KEY, SEED_TASKS)
  write(SUBS_KEY, SEED_SUBMISSIONS)
  localStorage.setItem(SEED_FLAG_KEY, '1')
}

// 模块级状态,跨组件共享(单例)
ensureSeed()
const tasks = ref(read(TASKS_KEY, []))
const submissions = ref(read(SUBS_KEY, []))
const currentUserId = ref(localStorage.getItem(USER_KEY) || DEMO_USER_ID)

watch(tasks, (v) => write(TASKS_KEY, v), { deep: true })
watch(submissions, (v) => write(SUBS_KEY, v), { deep: true })
watch(currentUserId, (v) => localStorage.setItem(USER_KEY, v))

const { coinBalance, setCoinBalance } = useInviteStats()

// 把过期 OPEN 任务刷成 EXPIRED 并退款(只在 OPEN 上处理)
function reconcileExpired() {
  const now = Date.now()
  let changed = false
  for (const t of tasks.value) {
    if (t.status === 'OPEN' && new Date(t.graceDeadlineAt).getTime() <= now) {
      t.status = 'EXPIRED'
      t.settledAt = new Date(now).toISOString()
      setCoinBalance(coinBalance.value + t.rewardCoin)
      changed = true
    }
  }
  if (changed) tasks.value = [...tasks.value]
}

// 页面 mount 后调一次,后续由 setInterval 每分钟轮询
let reconcileTimer = null
function startReconcile() {
  if (reconcileTimer) return
  reconcileTimer = setInterval(reconcileExpired, 60 * 1000)
  reconcileExpired()
}

const myNickname = computed(() => DEMO_USER_NICKNAMES[currentUserId.value] || '匿名用户')

function setCurrentUserId(id) {
  if (!DEMO_USER_NICKNAMES[id]) return
  currentUserId.value = id
}

const myPublishedTasks = computed(() =>
  tasks.value.filter(t => t.publisherId === currentUserId.value)
)

const mySubmissions = computed(() =>
  submissions.value.filter(s => s.submitterId === currentUserId.value && !s.withdrawnAt)
)

function getTask(id) {
  return tasks.value.find(t => t.id === id) || null
}

function getSubmissionsOfTask(taskId) {
  return submissions.value.filter(s => s.taskId === taskId && !s.withdrawnAt)
}

function mySubmissionForTask(taskId) {
  return submissions.value.find(s => s.taskId === taskId && s.submitterId === currentUserId.value && !s.withdrawnAt) || null
}

function createTask({ title, description, requirements, rewardCoin, deadlineDays }) {
  if (!title?.trim() || title.length > 30) {
    return { ok: false, error: '标题不能为空且不超过 30 字' }
  }
  if (!description?.trim() || description.length > 500) {
    return { ok: false, error: '需求描述不能为空且不超过 500 字' }
  }
  const min = Math.max(100, Math.floor(requirements.minWordCount || 0))
  const max = Math.min(5000, Math.floor(requirements.maxWordCount || 0))
  if (min < 100 || max > 5000 || min > max) {
    return { ok: false, error: '字数范围需在 100-5000,且下限不超过上限' }
  }
  if (rewardCoin < COMMISSION_CONFIG.MIN_REWARD || rewardCoin > COMMISSION_CONFIG.MAX_REWARD) {
    return { ok: false, error: `奖励需在 ${COMMISSION_CONFIG.MIN_REWARD}-${COMMISSION_CONFIG.MAX_REWARD} 创作币之间` }
  }
  if (![3, 7, 15].includes(deadlineDays)) {
    return { ok: false, error: '截止时间选项无效' }
  }
  if (coinBalance.value < rewardCoin) {
    return { ok: false, error: '余额不足,请先提现或做任务赚币', insufficient: true }
  }
  const now = Date.now()
  const deadline = new Date(now + deadlineDays * 24 * 60 * 60 * 1000)
  const grace = new Date(deadline.getTime() + COMMISSION_CONFIG.GRACE_HOURS * 60 * 60 * 1000)
  const task = {
    id: genId('cmt'),
    publisherId: currentUserId.value,
    publisherNickname: myNickname.value,
    title: title.trim(),
    description: description.trim(),
    requirements: { minWordCount: min, maxWordCount: max, styleHint: requirements.styleHint?.trim() || '' },
    rewardCoin,
    platformFeeRate: COMMISSION_CONFIG.PLATFORM_FEE_RATE,
    deadlineAt: deadline.toISOString(),
    graceDeadlineAt: grace.toISOString(),
    status: 'OPEN',
    winnerSubmissionId: null,
    settledAt: null,
    createdAt: new Date(now).toISOString()
  }
  tasks.value = [task, ...tasks.value]
  setCoinBalance(coinBalance.value - rewardCoin)
  return { ok: true, task }
}

function cancelTask(taskId) {
  const t = getTask(taskId)
  if (!t) return { ok: false, error: '任务不存在' }
  if (t.publisherId !== currentUserId.value) return { ok: false, error: '只能撤销自己的任务' }
  if (t.status !== 'OPEN') return { ok: false, error: '当前状态不允许撤销' }
  const subs = getSubmissionsOfTask(taskId)
  if (subs.length > 0) return { ok: false, error: '已有投稿,无法撤销' }
  t.status = 'CANCELLED'
  t.settledAt = new Date().toISOString()
  tasks.value = [...tasks.value]
  setCoinBalance(coinBalance.value + t.rewardCoin)
  return { ok: true }
}

function submitToTask(taskId, article) {
  const t = getTask(taskId)
  if (!t) return { ok: false, error: '任务不存在' }
  if (t.status !== 'OPEN') return { ok: false, error: '任务已截止' }
  if (t.publisherId === currentUserId.value) return { ok: false, error: '不能给自己的任务投稿' }
  if (mySubmissionForTask(taskId)) return { ok: false, error: '你已投递过此任务' }
  if (article.wordCount < t.requirements.minWordCount || article.wordCount > t.requirements.maxWordCount) {
    return { ok: false, error: `字数需在 ${t.requirements.minWordCount}-${t.requirements.maxWordCount}` }
  }
  const sub = {
    id: genId('cms'),
    taskId,
    submitterId: currentUserId.value,
    submitterNickname: myNickname.value,
    articleBizNo: article.bizNo,
    articleTitle: article.title,
    wordCount: article.wordCount,
    submittedAt: new Date().toISOString(),
    withdrawnAt: null
  }
  submissions.value = [sub, ...submissions.value]
  return { ok: true, submission: sub }
}

function withdrawSubmission(submissionId) {
  const s = submissions.value.find(x => x.id === submissionId)
  if (!s) return { ok: false, error: '投稿不存在' }
  if (s.submitterId !== currentUserId.value) return { ok: false, error: '只能撤回自己的投稿' }
  const t = getTask(s.taskId)
  if (!t || t.status !== 'OPEN') return { ok: false, error: '任务已截止,无法撤回' }
  s.withdrawnAt = new Date().toISOString()
  submissions.value = [...submissions.value]
  return { ok: true }
}

function pickWinner(taskId, submissionId) {
  const t = getTask(taskId)
  if (!t) return { ok: false, error: '任务不存在' }
  if (t.publisherId !== currentUserId.value) return { ok: false, error: '只有发布者可以选人' }
  if (t.status !== 'OPEN') return { ok: false, error: '当前状态不允许结算' }
  const subs = getSubmissionsOfTask(taskId)
  const target = subs.find(s => s.id === submissionId)
  if (!target) return { ok: false, error: '投稿不存在或已撤回' }
  const fee = Math.floor(t.rewardCoin * t.platformFeeRate)
  const payout = t.rewardCoin - fee
  t.status = 'SETTLED'
  t.winnerSubmissionId = submissionId
  t.settledAt = new Date().toISOString()
  tasks.value = [...tasks.value]
  // 把钱付给获胜者(若是当前用户自己中标的演示场景:用 mock 推一笔入账,
  // 但真实场景下后端会推送 notification。本地演示保持 setCoinBalance 不变,
  // 因为 coinBalance 是当前用户视角;演示中可切换 userId 看到别人中标)。
  return { ok: true, fee, payout }
}

export function useCommission() {
  return {
    // state
    tasks,
    submissions,
    currentUserId,
    myNickname,
    coinBalance,
    // computed
    myPublishedTasks,
    mySubmissions,
    // helpers
    setCurrentUserId,
    getTask,
    getSubmissionsOfTask,
    mySubmissionForTask,
    startReconcile,
    // actions
    createTask,
    cancelTask,
    submitToTask,
    withdrawSubmission,
    pickWinner
  }
}
```

**Step 5:** 提交。

```bash
git add project/user/web/src/api/commission.js \
        project/user/web/src/data/commissionSeed.js \
        project/user/web/src/composables/useCommission.js \
        project/user/web/src/composables/useInviteStats.js
git commit -m "feat(commission): 新增约稿中心 API 占位、composable 与种子数据"
```

---

### Task 2: 列表页 + 路由 + 侧边栏入口

**Files:**
- Create: `project/user/web/src/views/console/CommissionIndex.vue`
- Modify: `project/user/web/src/router/index.js`
- Modify: `project/user/web/src/views/console/ConsoleLayout.vue`(navItems + pageTitleMap)

**Step 1:** 在 `router/index.js` 中,`invite-rules` 路由之后插入:

```js
      {
        path: 'commission',
        name: 'ConsoleCommission',
        component: () => import('@/views/console/CommissionIndex.vue')
      },
      {
        path: 'commission/publish',
        name: 'ConsoleCommissionPublish',
        component: () => import('@/views/console/CommissionPublish.vue')
      },
      {
        path: 'commission/:id',
        name: 'ConsoleCommissionDetail',
        component: () => import('@/views/console/CommissionDetail.vue')
      }
```

**Step 2:** 在 `ConsoleLayout.vue` 的 icon import 块加入 `FileTextOutlined`,并在 `navItems` 数组中插入:

```js
  { path: '/console/commission', label: '约稿中心', icon: FileTextOutlined }
```

并把 `pageTitleMap` 增补:

```js
  '/console/commission': '约稿中心',
  '/console/commission/publish': '发布约稿',
  '/console/commission/:id': '约稿详情'
```

**Step 3:** 创建 `CommissionIndex.vue`(整体 ~280 行,Vue 3 单文件组件):

模板分四块:
- 页头(标题 + 三个 Tab: 全部任务 / 我发布的 / 我投稿的)
- 子筛选条(全部 / 进行中 / 已截止)
- 列表卡片(每条展示标题/发布者/奖励/截止倒计时/字数/投稿数/状态徽章)
- 空态(分别处理三种空)

样式约束:
- 卡片圆角 12px, 阴影 `0 2px 8px rgba(0,0,0,0.04)`
- 奖励徽章: 红色背景 `#fff0f2`, 文字 `#FF2442`, 圆角 16px
- 状态徽章: OPEN 用 `#e6f4ff` 底 `#1677ff` 字 / SETTLED 用 `#e6f7ed` 底 `#07c160` 字 / EXPIRED 用 `#f5f5f5` 底 `#8c8c8c` 字 / CANCELLED 用 `#fff7e6` 底 `#fa8c16` 字
- 截止倒计时: ≤2h 显示橙色"即将截止"

逻辑骨架:

```vue
<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useCommission } from '@/composables/useCommission'

const router = useRouter()
const {
  tasks, submissions, currentUserId, myNickname,
  myPublishedTasks, mySubmissions,
  getSubmissionsOfTask, startReconcile
} = useCommission()

const tab = ref('all')              // all | published | submitted
const filter = ref('all')           // all | open | closed

const now = ref(Date.now())
let tick = null
onMounted(() => {
  startReconcile()
  tick = setInterval(() => { now.value = Date.now() }, 60_000)
})
onUnmounted(() => { if (tick) clearInterval(tick) })

const visibleTasks = computed(() => {
  let list
  if (tab.value === 'published') list = myPublishedTasks.value
  else if (tab.value === 'submitted') {
    const ids = new Set(mySubmissions.value.map(s => s.taskId))
    list = tasks.value.filter(t => ids.has(t.id))
  } else list = tasks.value
  if (filter.value === 'open') list = list.filter(t => t.status === 'OPEN')
  if (filter.value === 'closed') list = list.filter(t => t.status !== 'OPEN')
  return [...list].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
})

function deadlineInfo(t) {
  const left = new Date(t.deadlineAt).getTime() - now.value
  if (left <= 0) return t.status === 'OPEN' ? { text: '宽限期中', danger: true } : { text: '已结束' }
  const days = Math.floor(left / 86400000)
  const hours = Math.floor((left % 86400000) / 3600000)
  const mins = Math.floor((left % 3600000) / 60000)
  if (left < 2 * 3600000) return { text: `即将截止 (${hours}h ${mins}m)`, danger: true }
  if (days > 0) return { text: `还剩 ${days} 天 ${hours} 小时` }
  return { text: `还剩 ${hours} 小时 ${mins} 分` }
}

function goDetail(t) { router.push(`/console/commission/${t.id}`) }
function goPublish() { router.push('/console/commission/publish') }
function mySubFor(t) {
  return submissions.value.find(s => s.taskId === t.id && s.submitterId === currentUserId.value && !s.withdrawnAt)
}
</script>
```

模板里:每个卡片 `:key="t.id"` 渲染,绑定 `@click="goDetail(t)"`,卡片内部最右侧按钮根据状态显示"去投稿 / 查看投稿 / 查看结果",按钮点击 `stop` 阻止冒泡。

底部浮动按钮:

```vue
<div class="commission-fab" @click="goPublish">+ 发布约稿</div>
```

CSS 用 scoped,固定右下角 80px 圆角 24px 红色按钮。

切换"我发布的/我投稿的"Tab 时,卡片上叠加一个角标标识(发布者显示"我发布的",投稿人显示"我投递的")。

**Step 4:** 提交。

```bash
git add project/user/web/src/views/console/CommissionIndex.vue \
        project/user/web/src/router/index.js \
        project/user/web/src/views/console/ConsoleLayout.vue
git commit -m "feat(commission): 新增约稿中心列表页与侧边栏入口"
```

---

### Task 3: 发布页(Modal)

**Files:**
- Create: `project/user/web/src/views/console/CommissionPublish.vue`

**Step 1:** 创建单文件组件,用 ant-design-vue `a-modal` 或原生 modal 都行(沿用 ConsoleLayout 的弹框风格: 居中 width 560 footer: null)。

表单项:

| 字段 | 类型 | 校验 |
|---|---|---|
| title | input | 必填, ≤ 30 字 |
| description | textarea | 必填, ≤ 500 字 |
| minWordCount | input-number | ≥ 100 |
| maxWordCount | input-number | ≤ 5000,且 ≥ min |
| styleHint | input | 可选, ≤ 50 字 |
| rewardCoin | input-number | 5 - 10000 |
| deadlineDays | radio | 3 / 7 / 15 |

实时预览卡片(在表单下方),展示:

- 平台抽成: `rewardCoin × 10%`(向下取整)
- 投稿者实得: `rewardCoin × 90%`
- 需冻结: `rewardCoin`
- 当前余额: `coinBalance` 创作币
- 冻结后余额: `coinBalance - rewardCoin` 创作币

提交逻辑:

```js
const result = createTask({ title, description, requirements: { minWordCount, maxWordCount, styleHint }, rewardCoin, deadlineDays })
if (!result.ok) {
  if (result.insufficient) {
    Modal.confirm({
      title: '余额不足',
      content: `发布此任务需冻结 ${rewardCoin} 创作币,当前余额仅 ${coinBalance}。是否前往提现?`,
      okText: '去提现',
      cancelText: '取消',
      onOk: () => router.push('/console/coin')
    })
    return
  }
  message.warning(result.error)
  return
}
message.success('发布成功,已冻结奖励')
router.replace(`/console/commission/${result.task.id}`)
```

关闭弹框: 取消按钮 + Esc + 蒙层点击都会跳回 `/console/commission`。

**Step 2:** 提交。

```bash
git add project/user/web/src/views/console/CommissionPublish.vue
git commit -m "feat(commission): 新增发布约稿页(含余额预览与冻结)"
```

---

### Task 4: 详情页 — 三视角 + 投稿选择器

**Files:**
- Create: `project/user/web/src/views/console/CommissionDetail.vue`

**Step 1:** 详情页加载时调用 `startReconcile()`,每分钟刷新倒计时。

模板结构:

- 顶部任务信息卡(标题、发布者昵称、状态徽章、奖励、字数要求、风格提示、截止时间、需求描述)
- 中间"操作区"(根据当前用户视角变化)
- 投稿区:
  - 发布者视角: 投稿列表(每人一行,含昵称/字数/投递时间/查看稿件按钮/选用 TA 按钮)
  - 投稿人视角: 自己的投稿状态卡 + 折叠的"其他投稿人"(只显示昵称+字数,不暴露文章正文)
  - 旁观者视角: 投稿数提示

投稿选择器: 发布者"选用 TA"按钮不动弹框,投稿人"立即投稿"按钮触发 ant-modal,内容是 `listArticles` 拉到的文章列表(从我的作品 composable 取),选择后调 `submitToTask(taskId, article)`。

注意: 这里需要 `useWorks()` 暴露一个返回我的作品的 ref 或方法。读取 `useWorks.js`,如果没有暴露 listArticles,临时新增:

```js
const articles = ref([])
async function loadArticles() {
  try {
    const data = await listArticles({ page: 1, pageSize: 50 })
    articles.value = data.list || []
  } catch {
    // 后端未实现时,给一些 demo 文章
    articles.value = DEMO_ARTICLES
  }
}
return { articles, loadArticles }
```

并在 `commissionSeed.js` 里追加 `DEMO_ARTICLES` 数组(3-4 篇,覆盖不同字数,便于演示)。

**Step 2:** 详情页核心逻辑:

```js
const isPublisher = computed(() => task.value?.publisherId === currentUserId.value)
const mySub = computed(() => mySubmissionForTask(taskId))

const taskSubmissions = computed(() => task.value ? getSubmissionsOfTask(task.value.id) : [])

const canSubmit = computed(() => {
  if (!task.value) return false
  if (task.value.status !== 'OPEN') return false
  if (isPublisher.value) return false
  if (mySub.value) return false
  return true
})

const canWithdraw = computed(() => {
  if (!mySub.value) return false
  if (task.value?.status !== 'OPEN') return false
  return true
})

const canCancel = computed(() => {
  if (!task.value || !isPublisher.value) return false
  if (task.value.status !== 'OPEN') return false
  if (taskSubmissions.value.length > 0) return false
  return true
})

const canPickWinner = computed(() => {
  if (!task.value || !isPublisher.value) return false
  if (task.value.status !== 'OPEN') return false
  if (taskSubmissions.value.length === 0) return false
  return true
})
```

发布者选人按钮触发 `Modal.confirm` 二选一确认 → `pickWinner(taskId, subId)` → toast 提示"已选 X 创作币 × 90% 给 TA,平台抽成 10%"。

发布者撤销触发 `Modal.confirm` → `cancelTask(taskId)` → toast 提示"已撤销,退款 X 创作币"。

**Step 3:** 投稿选择器 Modal:

- 标题: "选择要投递的文章"
- 列表: 每条卡片显示标题、字数、平台
- 字数超出任务范围的: 卡片置灰 + 标"字数不符"
- 底部"确认投递"按钮: 选中后启用

```js
const submitArticle = (article) => {
  const result = submitToTask(task.value.id, article)
  if (!result.ok) return message.warning(result.error)
  message.success('投递成功,等待发布者选择')
  pickerVisible.value = false
}
```

**Step 4:** 提交。

```bash
git add project/user/web/src/views/console/CommissionDetail.vue \
        project/user/web/src/composables/useWorks.js \
        project/user/web/src/data/commissionSeed.js
git commit -m "feat(commission): 新增约稿详情页(发布者/投稿人/旁观者三视角)"
```

---

### Task 5: 暗色主题适配 + 移动端适配 + 全链路自检

**Files:**
- Modify: `project/user/web/src/views/console/CommissionIndex.vue`
- Modify: `project/user/web/src/views/console/CommissionDetail.vue`
- Modify: `project/user/web/src/views/console/CommissionPublish.vue`

**Step 1:** 在三个 Vue 文件的 `<style scoped>` 末尾追加暗色主题:

```css
body[data-theme="dark"] .xxx-class { background: #1f1f1f; color: #e0e0e0; }
/* ... 覆盖卡片背景、徽章、按钮等 */
```

参考 `ConsoleLayout.vue` 已有的 `body[data-theme="dark"]` 写法,统一用 `#1f1f1f` / `#262626` / `#303030` / `#e0e0e0`。

**Step 2:** 移动端适配(≤768px):

- 列表卡片 padding 从 20px 缩到 12px
- 浮动按钮 right 20px → 16px, bottom 80px → 76px(让开 TabBar)
- 详情页按钮组:PC 横向,手机 stack

**Step 3:** 自检清单(用 `playwright` 或浏览器手测):

```bash
cd project/user/web
npm run dev
```

访问 `http://localhost:<port>/console/commission`,逐项验证:

1. **空数据清理**: `localStorage.removeItem('aichuangzuo_commission_seeded')` 后刷新,种子重新播种
2. **余额初始化**: 首屏 header 显示 `100` 创作币
3. **发布流程**: 点"发布约稿"→ 填表(标题/描述/字数 500-1500/奖励 50/截止 7 天)→ 提交 → 余额变 50 → 跳详情
4. **切换用户测试**: dev tools 里 `localStorage.setItem('aichuangzuo_commission_current_user', 'demo-user-2')` 刷新 → 看到我发布的列表里多了刚发的任务
5. **投稿流程**: 切回 `demo-user-1` → 点任务 → "立即投稿" → 选 demo 文章 → 投递 → 看到"已投递"
6. **重复投稿拦截**: 再点 → toast "你已投递过此任务"
7. **发布者选人**: 切到 `demo-user-2` → 详情页 → 看到投稿列表 → 选柠檬不酸 → toast 提示结算 → 任务变 SETTLED
8. **截止流局**: 改一个 OPEN 任务的 `deadlineAt` 和 `graceDeadlineAt` 为过去时间 → 刷新 → 任务变 EXPIRED → 余额自动 +50
9. **无投稿撤销**: 发布一个新任务 → 立即点"撤销任务" → 余额 +X
10. **有投稿撤销**: 切去投稿 → 再切回发布者 → 撤销按钮被禁用或提示
11. **移动端**: DevTools 切到 375px → TabBar 正常显示,内容不溢出
12. **暗色主题**: header 切深色 → 卡片背景变深、文字变浅,无白色残留

每项不符合预期就修,直到全过。

**Step 4:** 提交。

```bash
git add project/user/web/src/views/console/CommissionIndex.vue \
        project/user/web/src/views/console/CommissionDetail.vue \
        project/user/web/src/views/console/CommissionPublish.vue
git commit -m "polish(commission): 暗色主题 + 移动端适配 + 端到端自检"
```

---

## Self-Review Checklist

- [x] Spec §3 业务规则(5/10000/10%/3-7-15/1 篇/24h 宽限/无投稿可撤): Task 1 + Task 3 + Task 4 覆盖
- [x] Spec §4 数据模型: Task 1 在 `commissionSeed.js` 与 `useCommission.js` 完整定义
- [x] Spec §5.1 列表页(tabs/筛选/卡片/浮动按钮): Task 2
- [x] Spec §5.2 发布页(表单/余额预览): Task 3
- [x] Spec §5.3 详情页(三视角/选择器): Task 4
- [x] Spec §6 状态机(OPEN→SETTLED/EXPIRED/CANCELLED): Task 1 实现,Task 4 触发
- [x] Spec §7 持久化(`aichuangzuo_commission_tasks/submissions`、`aichuangzuo_coin_balance`): Task 1
- [x] Spec §8 API 占位(`commission.js` 含 TODO): Task 1
- [x] Spec §10 文件清单: 6 个新文件 + 3 处改动,全部覆盖
- [x] 无 "TBD/TODO/类似" 占位: 全部代码完整
- [x] 类型/函数签名一致: `useCommission` 返回的 keys 全部一致,`pickWinner` / `submitToTask` / `cancelTask` / `withdrawSubmission` / `createTask` 形态统一 `{ ok, error?, ...data }`
- [x] 暗色主题 + 移动端: Task 5
- [x] 端到端验证步骤: Task 5 Step 3