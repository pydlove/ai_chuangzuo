import { ref, computed } from 'vue'

const MARKET_KEY = 'aichuangzuo_style_market'
const EARNINGS_KEY = 'aichuangzuo_earnings_records'
const COIN_BALANCE_KEY = 'aichuangzuo_coin_balance'
const USER_ID_KEY = 'aichuangzuo_user_id'
const FAVORITES_KEY = 'aichuangzuo_favorite_styles'

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

export const marketStyles = ref(loadMarketStyles())
export const earningsRecords = ref(loadEarningsRecords())
export const favoriteIds = ref(loadFavoriteIds())

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

export function approveMarketStyle(marketId) {
  const s = marketStyles.value.find(x => x.id === marketId)
  if (s) {
    s.status = 'approved'
    saveMarketStyles()
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
        createdAt: now.toISOString()
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
