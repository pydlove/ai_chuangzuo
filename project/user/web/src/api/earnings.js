import { api } from '@/api/auth'

/**
 * 获取账户收益汇总。
 * @returns {Promise<{coinBalance:number, totalEarnings:number}>}
 */
export function getAccountSummary() {
  return api.get('/account/summary').then((res) => res.data || {})
}

/**
 * 获取按月收益汇总列表。
 * @returns {Promise<Array<{month:string, count:number, total:number}>>}
 */
export function getMonthlySettlementList() {
  return api.get('/account/monthly-summary').then((res) => res.data || [])
}

/**
 * 分页查询收益记录。
 * @param {{status?:string, month?:string, page?:number, pageSize?:number}} params
 * @returns {Promise<{list:Array, total:number, page:number, pageSize:number}>}
 */
export function getEarningsRecords(params = {}) {
  return api.get('/account/earnings', { params }).then((res) => res.data || { list: [], total: 0 })
}
