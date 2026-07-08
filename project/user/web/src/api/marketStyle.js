import { api } from '@/api/auth'

/**
 * 获取已上架的风格市场列表。
 * @returns {Promise<Array>}
 */
export function getMarketStyles() {
  return api.get('/market-styles').then((res) => {
    const list = res.data || res || []
    return list.map((s) => ({
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
      lastSettlementAt: s.lastSettlementAt,
      createdAt: s.createdAt
    }))
  })
}
