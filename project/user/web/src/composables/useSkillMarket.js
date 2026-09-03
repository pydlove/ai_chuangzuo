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

const PRICE_PER_USE = 2

export const pricePerUse = ref(PRICE_PER_USE)

export async function loadPricePerUse() {
  pricePerUse.value = PRICE_PER_USE
}

export const marketSkills = ref([])
export const marketOverview = ref({
  approvedCount: 0,
  totalUses: 0,
  totalEarnings: 0,
  featuredSkills: []
})
export const favoriteSkills = ref([])
export const mySubmissions = ref([])

export const favoriteIds = computed(() => favoriteSkills.value.map(s => s.id))

export async function loadMarketSkills() {
  try {
    marketSkills.value = await getMarketSkills()
  } catch (e) {
    // 加载失败时保持现有列表不变
  }
}

export async function loadMySubmissions() {
  try {
    mySubmissions.value = await getMyMarketSubmissions()
  } catch (e) {
    mySubmissions.value = []
  }
}

export async function loadFavoriteSkills(keyword = '', page = 1, pageSize = 999, updateGlobal = true) {
  try {
    const result = await getFavoriteSkills(keyword, page, pageSize)
    // 仅全量加载时同步全局 ref，分页调用由调用方自行维护局部状态
    if (updateGlobal && page === 1 && pageSize >= 999) {
      favoriteSkills.value = result.list || []
    }
    return result
  } catch (e) {
    if (updateGlobal) favoriteSkills.value = []
    return { list: [], total: 0, current: page, size: pageSize }
  }
}

export async function loadMarketSkillOverview() {
  try {
    marketOverview.value = await getMarketSkillOverview()
  } catch (e) {
    // 加载失败时保持概览不变
  }
}

export async function loadMarketSkillPage({ page = 1, pageSize = 15, keyword = '', sortType = 'all' } = {}) {
  try {
    return await getMarketSkillsPage({ page, pageSize, keyword, sortType })
  } catch (e) {
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
    // 操作失败时由调用方根据列表状态判断结果
  }
}

export function isFavorite(marketId) {
  return favoriteIds.value.includes(marketId)
}

/**
 * 下架自己发布的市场 skill。
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

// ===== 提示词市场视觉升级 v2 — 聚合 computed =====

export const marketStats = computed(() => ({
  approvedCount: Number(marketOverview.value.approvedCount || 0),
  totalUses: Number(marketOverview.value.totalUses || 0),
  totalEarnings: Number(marketOverview.value.totalEarnings || 0)
}))

export const featuredSkills = computed(() => marketOverview.value.featuredSkills || [])
