import { api } from '@/api/auth'

const DEFAULT_PAGE_SIZE = 15

function normalizeRow(s) {
  return {
    id: s.id,
    name: s.name,
    sourceType: s.sourceType,
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
    createdAt: s.createdAt
  }
}

function normalizeOverview(data) {
  return {
    approvedCount: data.approvedCount || 0,
    totalUses: data.totalUses || 0,
    totalEarnings: data.totalEarnings || 0,
    featuredSkills: (data.featuredSkills || []).map(normalizeRow),
    topCreators: (data.topCreators || []).map((c) => ({
      creatorId: c.creatorId,
      creatorName: c.creatorName,
      weeklyEarnings: c.weeklyEarnings || 0,
      weeklyUses: c.weeklyUses || 0,
      totalEarnings: c.totalEarnings || 0,
      bestSkill: c.bestSkill ? normalizeRow(c.bestSkill) : null
    }))
  }
}

/**
 * 获取全部已上架 skills 市场列表（兼容旧逻辑）。
 * @returns {Promise<Array>}
 */
export function getMarketSkills() {
  return api.get('/market-skills').then((res) => {
    const list = res.data || res || []
    return list.map(normalizeRow)
  })
}

/**
 * 获取 skills 市场概览（统计、官方精选、收益潜力榜）。
 * @returns {Promise<Object>}
 */
export function getMarketSkillOverview() {
  return api.get('/market-skills/overview').then((res) => {
    const data = res.data || res || {}
    return normalizeOverview(data)
  })
}

/**
 * 分页查询已上架 skills 市场列表。
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
