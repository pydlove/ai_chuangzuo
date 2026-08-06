import { api } from '@/api/auth'

const DEFAULT_PAGE_SIZE = 15

function normalizeRow(s) {
  return {
    id: s.id,
    name: s.name,
    sourceType: s.sourceType,
    description: s.description || s.desc || '',
    desc: s.description || s.desc || '',
    promptSummary: s.promptSummary || '',
    creatorId: s.creatorId,
    creatorName: s.creatorName,
    prompt: s.prompt,
    scope: s.scope,
    excerpt1: s.excerpt1,
    excerpt2: s.excerpt2,
    status: s.status,
    price: s.price,
    weeklyUses: s.weeklyUses,
    totalUses: s.totalUses,
    weeklyEarnings: s.weeklyEarnings,
    milestoneBonus: s.milestoneBonus,
    featured: s.featured === true,
    lastSettlementAt: s.lastSettlementAt,
    approvedAt: s.approvedAt,
    createdAt: s.createdAt
  }
}

function normalizeOverview(data) {
  if (!data) return null
  return {
    approvedCount: data.approvedCount || 0,
    totalUses: data.totalUses || 0,
    totalEarnings: data.totalEarnings || 0,
    featuredSkills: (data.featuredSkills || []).map(normalizeRow)
  }
}

/**
 * 获取全部已上架提示词市场列表（兼容旧逻辑）。
 * @returns {Promise<Array>}
 */
export function getMarketSkills() {
  return api.get('/market-skills').then((res) => {
    const list = res.data || res || []
    return list.map(normalizeRow)
  })
}

/**
 * 获取提示词市场概览（统计、官方精选）。
 * @returns {Promise<Object>}
 */
export function getMarketSkillOverview() {
  return api.get('/market-skills/overview').then((res) => {
    const data = res.data || res || {}
    return normalizeOverview(data)
  })
}

/**
 * 获取当前用户收藏的市场 skill 详情列表（含已下架）。
 * @returns {Promise<Array>}
 */
export function getFavoriteSkills() {
  return api.get('/market-skills/favorites').then((res) => {
    const data = res.data || res || []
    return Array.isArray(data) ? data.map(normalizeRow) : []
  })
}

/**
 * 获取当前用户的市场提交记录（含待审核/已通过/已打回）。
 * @returns {Promise<Array>}
 */
export function getMyMarketSubmissions() {
  return api.get('/market-skills/my-submissions').then((res) => {
    const data = res.data || res || []
    return Array.isArray(data) ? data.map(normalizeRow) : []
  })
}

/**
 * 下架自己发布的市场 skill。
 * @param {string} marketSkillId
 */
export function deleteMarketSkill(marketSkillId) {
  return api.delete(`/market-skills/${marketSkillId}`)
}

/**
 * 收藏市场 skill。
 * @param {string} marketSkillId
 */
export function addFavorite(marketSkillId) {
  return api.post(`/market-skills/favorites/${marketSkillId}`)
}

/**
 * 取消收藏市场 skill。
 * @param {string} marketSkillId
 */
export function removeFavorite(marketSkillId) {
  return api.delete(`/market-skills/favorites/${marketSkillId}`)
}

/**
 * 分页查询已上架提示词市场列表。
 * @param {Object} params
 * @param {number} [params.page=1]
 * @param {number} [params.pageSize=15]
 * @param {string} [params.keyword='']
 * @param {string} [params.sortType='all']
 * @returns {Promise<{list: Array, total: number, current: number, size: number}>}
 */
export function getMarketSkillsPage({
  page = 1,
  pageSize = DEFAULT_PAGE_SIZE,
  keyword = '',
  sortType = 'all'
} = {}) {
  return api
    .get('/market-skills/paged', {
      params: {
        page,
        pageSize,
        keyword: keyword || undefined,
        sortType: sortType || 'all'
      }
    })
    .then((res) => {
      const data = res.data || res || {}
      const records = data.records || data.list || []
      return {
        list: records.map(normalizeRow),
        total: data.total || 0,
        current: data.current || page,
        size: data.size || pageSize
      }
    })
}
