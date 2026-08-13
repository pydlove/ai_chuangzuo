import { ref, computed } from 'vue'
import {
  getMarketSkills,
  getMarketSkillOverview,
  getMarketSkillsPage,
  getFavoriteSkills,
  getMyMarketSubmissions,
  addFavorite,
  removeFavorite,
  deleteMarketSkill
} from '@/api/marketSkill.js'

const EARNINGS_KEY = 'aichuangzuo_earnings_records'
const COIN_BALANCE_KEY = 'aichuangzuo_coin_balance'
const USER_ID_KEY = 'aichuangzuo_user_id'

const PRICE_PER_USE = 2

export const pricePerUse = ref(PRICE_PER_USE)

export async function loadPricePerUse() {
  pricePerUse.value = PRICE_PER_USE
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
  featuredSkills: []
})
export const earningsRecords = ref(loadEarningsRecords())
export const favoriteSkills = ref([])
export const favoriteIds = computed(() => favoriteSkills.value.map(s => s.id))
export const mySubmissions = ref([])

export async function loadMarketSkills() {
  try {
    marketSkills.value = await getMarketSkills()
  } catch (e) {
    console.warn('[loadMarketSkills]', e?.message || '加载失败')
  }
}

export async function loadMySubmissions() {
  try {
    mySubmissions.value = await getMyMarketSubmissions()
  } catch (e) {
    console.warn('[loadMySubmissions]', e?.message || '加载失败')
    mySubmissions.value = []
  }
}

export async function loadFavoriteSkills(keyword = '', page = 1, pageSize = 999) {
  try {
    const result = await getFavoriteSkills(keyword, page, pageSize)
    // 仅全量加载时同步全局 ref，分页调用由调用方自行维护局部状态
    if (page === 1 && pageSize >= 999) {
      favoriteSkills.value = result.list || []
    }
    return result
  } catch (e) {
    console.warn('[loadFavoriteSkills]', e?.message || '加载失败')
    favoriteSkills.value = []
    return { list: [], total: 0, current: page, size: pageSize }
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

export async function toggleFavorite(marketId) {
  const currentlyFavorite = favoriteIds.value.includes(marketId)
  try {
    if (currentlyFavorite) {
      await removeFavorite(marketId)
      favoriteSkills.value = favoriteSkills.value.filter((s) => s.id !== marketId)
    } else {
      await addFavorite(marketId)
      const skill = marketSkills.value.find((s) => s.id === marketId)
      if (skill && !favoriteSkills.value.some((s) => s.id === marketId)) {
        favoriteSkills.value = [...favoriteSkills.value, skill]
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

/**
 * 下架自己发布的市场 skill（包括本地 mock 与后端已入库的）。
 * @param {string} bizNo 源 skill 业务编号
 */
export async function unpublishSkill(bizNo) {
  if (!bizNo) return
  await deleteMarketSkill(bizNo)
  marketSkills.value = marketSkills.value.filter(s => s.id !== bizNo)
  mySubmissions.value = mySubmissions.value.filter(s => s.id !== bizNo)
}

export function getMarketStatusByBizNo(bizNo) {
  const s = mySubmissions.value.find((m) => m.id === bizNo)
  if (!s) return ''
  if (s.status === 'pending') return '审核中'
  if (s.status === 'approved') return '已上架'
  if (s.status === 'rejected') return '已打回'
  return ''
}

export function useMarketSkill(marketId) {
  const s = marketSkills.value.find(x => x.id === marketId)
  if (!s) throw new Error('skill 不存在')
  if (s.status !== 'approved') throw new Error('skill 未上架')

  // 前端 mock：使用他人分享的 skill 不扣创作币，创作者仍获得收益
  const price = Number(s.price || pricePerUse.value)
  const creatorBalance = getCoinBalance()
  setCoinBalance(Number((creatorBalance + price).toFixed(2)))

  s.totalUses = (s.totalUses || 0) + 1

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

  s.totalUses = (s.totalUses || 0) + 1

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

export const marketStats = computed(() => ({
  approvedCount: Number(marketOverview.value.approvedCount || 0),
  totalUses: Number(marketOverview.value.totalUses || 0),
  totalEarnings: Number(marketOverview.value.totalEarnings || 0)
}))

export const featuredSkills = computed(() => marketOverview.value.featuredSkills || [])
