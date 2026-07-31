import { ref, computed } from 'vue'
import {
  getMarketSkills,
  getMarketSkillOverview,
  getMarketSkillsPage,
  getFavoriteIds,
  getMarketSkillPricePerUse,
  addFavorite,
  removeFavorite
} from '@/api/marketSkill.js'
import { useBenefits } from '@/composables/useBenefits.js'

const EARNINGS_KEY = 'aichuangzuo_earnings_records'
const COIN_BALANCE_KEY = 'aichuangzuo_coin_balance'
const USER_ID_KEY = 'aichuangzuo_user_id'

const PRICE_PER_USE = 2

export const pricePerUse = ref(PRICE_PER_USE)

export async function loadPricePerUse() {
  try {
    const value = await getMarketSkillPricePerUse()
    pricePerUse.value = value
  } catch (e) {
    console.warn('[loadPricePerUse]', e?.message || '加载失败')
    pricePerUse.value = PRICE_PER_USE
  }
}

function loadEarningsRecords() {
  try {
    const raw = localStorage.getItem(EARNINGS_KEY)
    const records = raw ? JSON.parse(raw) : []
    const currentMonth = getCurrentMonth()
    return records.map((r) => {
      const month = r.settlementMonth || (r.createdAt ? getMonthFromDate(new Date(r.createdAt)) : currentMonth)
      return {
        ...r,
        status: r.status || (month < currentMonth ? 'settled' : 'unsettled'),
        settlementMonth: month
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

export function getCoinBalance() {
  const raw = localStorage.getItem(COIN_BALANCE_KEY)
  return raw ? parseFloat(raw) : 0
}

function setCoinBalance(balance) {
  localStorage.setItem(COIN_BALANCE_KEY, String(balance))
}

export const marketSkills = ref([])
export const marketOverview = ref({
  approvedCount: 0,
  totalUses: 0,
  totalEarnings: 0,
  featuredSkills: [],
  topCreators: []
})
export const earningsRecords = ref(loadEarningsRecords())
export const favoriteIds = ref([])

export async function loadMarketSkills() {
  try {
    marketSkills.value = await getMarketSkills()
  } catch (e) {
    console.warn('[loadMarketSkills]', e?.message || '加载失败')
  }
}

export async function loadFavoriteIds() {
  try {
    favoriteIds.value = await getFavoriteIds()
  } catch (e) {
    console.warn('[loadFavoriteIds]', e?.message || '加载失败')
    favoriteIds.value = []
  }
}

export async function loadMarketSkillOverview() {
  try {
    marketOverview.value = await getMarketSkillOverview()
  } catch (e) {
    console.warn('[loadMarketSkillOverview]', e?.message || '加载失败')
  }
}

export async function loadMarketSkillPage({ page = 1, pageSize = 15, keyword = '', sortType = 'all' } = {}) {
  try {
    return await getMarketSkillsPage({ page, pageSize, keyword, sortType })
  } catch (e) {
    console.warn('[loadMarketSkillPage]', e?.message || '加载失败')
    return { list: [], total: 0, current: page, size: pageSize }
  }
}

export const favoriteSkills = computed(() =>
  marketSkills.value.filter(s => s.status === 'approved' && favoriteIds.value.includes(s.id))
)

export async function toggleFavorite(marketId) {
  const currentlyFavorite = favoriteIds.value.includes(marketId)
  try {
    if (currentlyFavorite) {
      await removeFavorite(marketId)
      favoriteIds.value = favoriteIds.value.filter((id) => id !== marketId)
    } else {
      await addFavorite(marketId)
      if (!favoriteIds.value.includes(marketId)) {
        favoriteIds.value = [...favoriteIds.value, marketId]
      }
    }
  } catch (e) {
    console.warn('[toggleFavorite]', e?.message || '操作失败')
  }
}

export function isFavorite(marketId) {
  return favoriteIds.value.includes(marketId)
}

function getMonthFromDate(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

function getCurrentMonth() {
  return getMonthFromDate(new Date())
}

/** 当前用户本月已发布到市场的 skills 数量（按 marketSkills 实时统计）。 */
export function countMyPublishesThisMonth() {
  const uid = getUserId()
  const month = getCurrentMonth()
  return marketSkills.value.filter(
    s => s.creatorId === uid && (s.createdAt || '').startsWith(month)
  ).length
}

/** 当前档位本月还可发布多少次；0 表示禁止发布或额度已用完。 */
export function getRemainingPublishQuota() {
  const { benefitRemaining } = useBenefits()
  return Math.max(benefitRemaining('skill_market_publish'), 0)
}

export function shareSkillToMarket(style, sourceType) {
  const { benefitRemaining, benefitValue } = useBenefits()
  const remaining = benefitRemaining('skill_market_publish')
  if (remaining <= 0) {
    const quota = parseInt(benefitValue('skill_market_publish') || '0', 10)
    if (quota <= 0) {
      throw new Error('当前套餐不支持发布 skills 到 提示词市场，请升级会员')
    }
    throw new Error(`本月发布额度已用完（${quota} 次），下月 1 日重置`)
  }

  const existing = marketSkills.value.find(
    s => s.originalName === style.name && s.creatorId === getUserId() && s.sourceType === sourceType
  )
  if (existing) {
    throw new Error('该 skill 已经分享过')
  }
  const id = 'market-' + Date.now().toString(36)
  marketSkills.value.unshift({
    id,
    name: style.name,
    sourceType,
    originalName: style.name,
    creatorId: getUserId(),
    creatorName: '我',
    prompt: style.prompt,
    description: style.description || style.desc || '',
    scope: style.scope || '',
    excerpt1: style.excerpt1 || '',
    excerpt2: style.excerpt2 || '',
    status: 'pending',
    featured: false,
    price: pricePerUse.value,
    weeklyUses: 0,
    totalUses: 0,
    weeklyEarnings: 0,
    milestoneBonus: 0,
    monthlyUses: 0,
    monthlyEarnings: 0,
    leaderboardReward: 0,
    lastSettlementAt: new Date().toISOString(),
    createdAt: new Date().toISOString()
  })
  return id
}

export function useMarketSkill(marketId) {
  const s = marketSkills.value.find(x => x.id === marketId)
  if (!s) throw new Error('skill 不存在')
  if (s.status !== 'approved') throw new Error('skill 未上架')

  // 前端 mock：使用他人分享的 skill 不扣创作币，创作者仍获得收益
  const price = Number(s.price || pricePerUse.value)
  const creatorBalance = getCoinBalance()
  setCoinBalance(Number((creatorBalance + price).toFixed(2)))

  s.monthlyUses = (s.monthlyUses || 0) + 1
  s.totalUses = (s.totalUses || 0) + 1
  s.monthlyEarnings = Number(((s.monthlyUses || 0) * price).toFixed(2))

  earningsRecords.value.unshift({
    id: 'earn-' + Date.now().toString(36),
    type: 'usage',
    skillName: s.name,
    skillId: s.id,
    amount: price,
    fromUserId: getUserId(),
    description: `使用「${s.name}」生成文章`,
    status: 'unsettled',
    settlementMonth: getCurrentMonth(),
    createdAt: new Date().toISOString()
  })
  saveEarningsRecords()
}

export function simulateExternalUse(marketId) {
  const s = marketSkills.value.find(x => x.id === marketId)
  if (!s) throw new Error('skill 不存在')
  if (s.status !== 'approved') throw new Error('skill 未上架')

  // 前端 mock：外部用户使用免费，创作者获得收益
  const price = Number(s.price || pricePerUse.value)
  const creatorBalance = getCoinBalance()
  setCoinBalance(Number((creatorBalance + price).toFixed(2)))

  s.monthlyUses = (s.monthlyUses || 0) + 1
  s.totalUses = (s.totalUses || 0) + 1
  s.monthlyEarnings = Number(((s.monthlyUses || 0) * price).toFixed(2))

  earningsRecords.value.unshift({
    id: 'earn-' + Date.now().toString(36),
    type: 'usage',
    skillName: s.name,
    skillId: s.id,
    amount: price,
    fromUserId: 'external-user',
    description: `其他用户使用「${s.name}」生成文章`,
    status: 'unsettled',
    settlementMonth: getCurrentMonth(),
    createdAt: new Date().toISOString()
  })
  saveEarningsRecords()
}

export function getMarketStyleEarnings(marketId) {
  return earningsRecords.value
    .filter(r => r.skillId === marketId && r.amount > 0)
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

export function getMonthlyEarnings(month) {
  return earningsRecords.value
    .filter(r => r.settlementMonth === month && r.amount > 0)
    .reduce((sum, r) => sum + r.amount, 0)
}

export function getCurrentMonthEarnings() {
  return getMonthlyEarnings(getCurrentMonth())
}

export function getMonthlySettlementList() {
  const map = new Map()
  earningsRecords.value
    .filter(r => r.amount > 0)
    .forEach((r) => {
      const month = r.settlementMonth || getMonthFromDate(new Date(r.createdAt))
      if (!map.has(month)) {
        map.set(month, { month, total: 0, settled: 0, unsettled: 0, count: 0 })
      }
      const item = map.get(month)
      item.total += r.amount
      item.count += 1
      if (r.status === 'settled') {
        item.settled += r.amount
      } else {
        item.unsettled += r.amount
      }
    })
  return Array.from(map.values()).sort((a, b) => b.month.localeCompare(a.month))
}

export function monthlySettle(targetMonth) {
  const month = targetMonth || getPreviousMonth()
  let settled = 0
  earningsRecords.value.forEach((r) => {
    if (r.settlementMonth === month && r.status === 'unsettled') {
      r.status = 'settled'
      settled += r.amount
    }
  })
  if (settled > 0) {
    saveEarningsRecords()
  }
  return settled
}

export function getPreviousMonth() {
  const now = new Date()
  now.setMonth(now.getMonth() - 1)
  return getMonthFromDate(now)
}

// ===== 提示词市场视觉升级 v2 — 聚合 computed =====
// 数据来源原则：统计/榜单/精选从 marketOverview 读取；
// monthlyUses / totalUses / monthlyEarnings 取 marketSkills 单条；
// totalEarnings（创作者总收益）取 earningsRecords，按 skillId → creatorId 关联。

const totalEarningsByCreator = computed(() => {
  const map = {}
  earningsRecords.value.forEach((r) => {
    if (!(r.amount > 0)) return
    const s = marketSkills.value.find((m) => m.id === r.skillId)
    if (!s) return
    map[s.creatorId] = (map[s.creatorId] || 0) + r.amount
  })
  return map
})

export const marketStats = computed(() => ({
  approvedCount: Number(marketOverview.value.approvedCount || 0),
  totalUses: Number(marketOverview.value.totalUses || 0),
  totalEarnings: Number(marketOverview.value.totalEarnings || 0)
}))

export const topCreators = computed(() =>
  (marketOverview.value.topCreators || []).map((c) => ({
    ...c,
    totalEarnings: Number(c.totalEarnings || 0)
  }))
)

export const featuredSkills = computed(() => marketOverview.value.featuredSkills || [])
