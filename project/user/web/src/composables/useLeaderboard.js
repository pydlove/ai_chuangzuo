import { ref } from 'vue'
import { earningsRecords } from '@/composables/useStyleMarket.js'

const INCOME_SUBMISSIONS_KEY = 'aichuangzuo_leaderboard_income_submissions'
const REWARD_RECORDS_KEY = 'aichuangzuo_leaderboard_rewards'
const EARNINGS_KEY = 'aichuangzuo_earnings_records'
const COIN_BALANCE_KEY = 'aichuangzuo_coin_balance'
const USER_ID_KEY = 'aichuangzuo_user_id'

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

function getPeriodRange(periodType, periodValue) {
  if (periodType === 'month') {
    const start = new Date(`${periodValue}-01T00:00:00`)
    const end = new Date(start.getFullYear(), start.getMonth() + 1, 1)
    return { start, end }
  }
  const year = parseInt(periodValue, 10)
  return { start: new Date(`${year}-01-01T00:00:00`), end: new Date(`${year + 1}-01-01T00:00:00`) }
}

function generateMockIncomeUser(index, periodType, periodValue) {
  const userId = 'mock_income_' + index
  const nickname = MOCK_NICKNAMES[(index + 5) % MOCK_NICKNAMES.length]
  const seed = hashString(`${periodType}-${periodValue}-${userId}`)
  const factor = periodType === 'year' ? 12 : 1
  const amount = Number(((seededRandom(seed) * 10000 + 500) * factor).toFixed(2))
  return { userId, nickname, amount }
}

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

export function submitIncomeSubmission(payload) {
  if (!/^\d{4}-\d{2}$/.test(payload.month)) {
    throw new Error('month must be YYYY-MM format')
  }
  if (!Number.isFinite(Number(payload.amount))) {
    throw new Error('amount must be a finite number')
  }
  if (!payload.screenshot || typeof payload.screenshot !== 'string') {
    throw new Error('screenshot is required')
  }
  const submission = {
    id: 'income-' + Date.now().toString(36),
    userId: getUserId(),
    month: payload.month,
    amount: Math.round(Number(payload.amount) * 100) / 100,
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
    list.push(generateMockIncomeUser(i, periodType, periodValue))
  }

  list.sort((a, b) => b.amount - a.amount)
  return list.map((item, index) => ({ ...item, rank: index + 1 }))
}

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
      const earningRecord = {
        id: 'earn-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 6),
        type: 'leaderboard_reward',
        styleName: '',
        styleId: '',
        amount: 100,
        description: `${leaderboardType === 'coin' ? '创作币榜' : '自媒体收入榜'} 月度第 ${item.rank} 名奖励`,
        status: 'settled',
        settlementWeek: '',
        createdAt: new Date().toISOString()
      }
      earnings.unshift(earningRecord)
      save(EARNINGS_KEY, earnings)
      if (earningsRecords?.value) {
        earningsRecords.value.unshift(earningRecord)
      }
    }

    awardedCount++
  })

  if (awardedCount > 0) {
    save(REWARD_RECORDS_KEY, rewardRecords.value)
  }
  return awardedCount
}

export function getRewardRecord(leaderboardType, periodValue, userId) {
  return rewardRecords.value.find(
    r => r.leaderboardType === leaderboardType && r.periodValue === periodValue && r.userId === userId
  )
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
