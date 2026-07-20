import { ref, computed } from 'vue'
import { getMarketStyles } from '@/api/marketStyle.js'
import { getStylePublishQuota } from '@/utils/membershipLimits.js'

const EARNINGS_KEY = 'aichuangzuo_earnings_records'
const COIN_BALANCE_KEY = 'aichuangzuo_coin_balance'
const USER_ID_KEY = 'aichuangzuo_user_id'
const FAVORITES_KEY = 'aichuangzuo_favorite_styles'

const PRICE_PER_USE = 0.2

function loadEarningsRecords() {
  try {
    const raw = localStorage.getItem(EARNINGS_KEY)
    const records = raw ? JSON.parse(raw) : []
    const currentWeek = getCurrentWeek()
    return records.map((r) => {
      const week = r.settlementWeek || (r.createdAt ? getWeekFromDate(new Date(r.createdAt)) : currentWeek)
      return {
        ...r,
        status: r.status || (week < currentWeek ? 'settled' : 'unsettled'),
        settlementWeek: week
      }
    })
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

function loadFavoriteIds() {
  try {
    const raw = localStorage.getItem(FAVORITES_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveFavoriteIds() {
  localStorage.setItem(FAVORITES_KEY, JSON.stringify(favoriteIds.value))
}

export function getCoinBalance() {
  const raw = localStorage.getItem(COIN_BALANCE_KEY)
  return raw ? parseFloat(raw) : 0
}

function setCoinBalance(balance) {
  localStorage.setItem(COIN_BALANCE_KEY, String(balance))
}

export const marketStyles = ref([])
export const earningsRecords = ref(loadEarningsRecords())
export const favoriteIds = ref(loadFavoriteIds())

export async function loadMarketStyles() {
  try {
    marketStyles.value = await getMarketStyles()
  } catch (e) {
    console.warn('[loadMarketStyles]', e?.message || '加载失败')
  }
}

export const favoriteStyles = computed(() =>
  marketStyles.value.filter(s => s.status === 'approved' && favoriteIds.value.includes(s.id))
)

export function toggleFavorite(marketId) {
  const set = new Set(favoriteIds.value)
  if (set.has(marketId)) {
    set.delete(marketId)
  } else {
    set.add(marketId)
  }
  favoriteIds.value = Array.from(set)
  saveFavoriteIds()
}

export function isFavorite(marketId) {
  return favoriteIds.value.includes(marketId)
}

function getWeekFromDate(date) {
  const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()))
  const dayNum = d.getUTCDay() || 7
  d.setUTCDate(d.getUTCDate() + 4 - dayNum)
  const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1))
  const weekNo = Math.ceil((((d - yearStart) / 86400000) + 1) / 7)
  return `${d.getUTCFullYear()}-W${String(weekNo).padStart(2, '0')}`
}

function getCurrentWeek() {
  return getWeekFromDate(new Date())
}

/** 当前 yyyy-MM 字符串，用于「本月已发布数」计数隔离。 */
function getCurrentMonth() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

/** 当前用户本月已发布到市场的风格数（按 marketStyles 实时统计）。 */
export function countMyPublishesThisMonth() {
  const uid = getUserId()
  const month = getCurrentMonth()
  return marketStyles.value.filter(
    s => s.creatorId === uid && (s.createdAt || '').startsWith(month)
  ).length
}

/** 当前档位本月还可发布多少次；-1 表示不限制（套餐不限），0 表示禁止发布。 */
export function getRemainingPublishQuota() {
  const quota = getStylePublishQuota()
  if (quota <= 0) return 0
  return Math.max(quota - countMyPublishesThisMonth(), 0)
}

export function shareStyleToMarket(style, sourceType) {
  const remaining = getRemainingPublishQuota()
  if (remaining <= 0) {
    const quota = getStylePublishQuota()
    if (quota <= 0) {
      throw new Error('当前套餐不支持发布风格到市场，请升级会员')
    }
    throw new Error(`本月发布额度已用完（${quota} 次），下月 1 日重置`)
  }

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
  return id
}

export function approveMarketStyle(marketId) {
  const s = marketStyles.value.find(x => x.id === marketId)
  if (s) {
    s.status = 'approved'
    }
}

export function useMarketStyle(marketId) {
  const s = marketStyles.value.find(x => x.id === marketId)
  if (!s) throw new Error('风格不存在')
  if (s.status !== 'approved') throw new Error('风格未上架')

  // 前端 mock：使用他人分享的风格不扣创作币，创作者仍获得收益
  const creatorBalance = getCoinBalance()
  setCoinBalance(Number((creatorBalance + PRICE_PER_USE).toFixed(2)))

  s.weeklyUses += 1
  s.totalUses += 1
  s.weeklyEarnings = Number((s.weeklyUses * PRICE_PER_USE).toFixed(2))

  earningsRecords.value.unshift({
    id: 'earn-' + Date.now().toString(36),
    type: 'usage',
    styleName: s.name,
    styleId: s.id,
    amount: PRICE_PER_USE,
    fromUserId: getUserId(),
    description: `使用「${s.name}」生成文章`,
    status: 'unsettled',
    settlementWeek: getCurrentWeek(),
    createdAt: new Date().toISOString()
  })
  saveEarningsRecords()
}

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
        status: 'unsettled',
        settlementWeek: getCurrentWeek(),
        createdAt: now.toISOString()
      })
      saveEarningsRecords()
    }

    s.weeklyUses = 0
    s.weeklyEarnings = 0
    s.milestoneBonus = 0
    s.lastSettlementAt = now.toISOString()
  })
}

export function simulateExternalUse(marketId) {
  const s = marketStyles.value.find(x => x.id === marketId)
  if (!s) throw new Error('风格不存在')
  if (s.status !== 'approved') throw new Error('风格未上架')

  // 前端 mock：外部用户使用免费，创作者获得收益
  const creatorBalance = getCoinBalance()
  setCoinBalance(Number((creatorBalance + PRICE_PER_USE).toFixed(2)))

  s.weeklyUses += 1
  s.totalUses += 1
  s.weeklyEarnings = Number((s.weeklyUses * PRICE_PER_USE).toFixed(2))

  earningsRecords.value.unshift({
    id: 'earn-' + Date.now().toString(36),
    type: 'usage',
    styleName: s.name,
    styleId: s.id,
    amount: PRICE_PER_USE,
    fromUserId: 'external-user',
    description: `其他用户使用「${s.name}」生成文章`,
    status: 'unsettled',
    settlementWeek: getCurrentWeek(),
    createdAt: new Date().toISOString()
  })
  saveEarningsRecords()
}

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

export function getSettledEarnings() {
  return earningsRecords.value
    .filter(r => r.status === 'settled' && r.amount > 0)
    .reduce((sum, r) => sum + r.amount, 0)
}

export function getUnsettledEarnings() {
  return earningsRecords.value
    .filter(r => r.status === 'unsettled' && r.amount > 0)
    .reduce((sum, r) => sum + r.amount, 0)
}

export function getWeeklyEarnings(week) {
  return earningsRecords.value
    .filter(r => r.settlementWeek === week && r.amount > 0)
    .reduce((sum, r) => sum + r.amount, 0)
}

export function getCurrentWeekEarnings() {
  return getWeeklyEarnings(getCurrentWeek())
}

export function getWeeklySettlementList() {
  const map = new Map()
  earningsRecords.value
    .filter(r => r.amount > 0)
    .forEach((r) => {
      const week = r.settlementWeek || getWeekFromDate(new Date(r.createdAt))
      if (!map.has(week)) {
        map.set(week, { week, total: 0, settled: 0, unsettled: 0, count: 0 })
      }
      const item = map.get(week)
      item.total += r.amount
      item.count += 1
      if (r.status === 'settled') {
        item.settled += r.amount
      } else {
        item.unsettled += r.amount
      }
    })
  return Array.from(map.values()).sort((a, b) => b.week.localeCompare(a.week))
}

export function weeklySettle(targetWeek) {
  const week = targetWeek || getPreviousWeek()
  let settled = 0
  earningsRecords.value.forEach((r) => {
    if (r.settlementWeek === week && r.status === 'unsettled') {
      r.status = 'settled'
      settled += r.amount
    }
  })
  if (settled > 0) {
    saveEarningsRecords()
  }
  return settled
}

export function getPreviousWeek() {
  const now = new Date()
  now.setDate(now.getDate() - 7)
  return getWeekFromDate(now)
}
