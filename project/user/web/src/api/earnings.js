import { api } from '@/api/auth'

/**
 * 获取账户收益汇总。
 * @returns {Promise<{coinBalance:number, totalEarnings:number, settledEarnings:number, unsettledEarnings:number}>}
 */
export function getAccountSummary() {
  return api.get('/account/summary').then((res) => res.data || {})
}

/**
 * 获取按月结算列表。
 * @returns {Promise<Array<{month:string, count:number, total:number, settled:number, unsettled:number}>>}
 */
export function getMonthlySettlementList() {
  return api.get('/account/settlement-list').then((res) => res.data || [])
}

/**
 * 分页查询收益记录。
 * @param {{status?:string, month?:string, page?:number, pageSize?:number}} params
 * @returns {Promise<{list:Array, total:number, page:number, pageSize:number}>}
 */
export function getEarningsRecords(params = {}) {
  return api.get('/account/earnings', { params }).then((res) => res.data || { list: [], total: 0 })
}

/**
 * 结算上月收益。
 * @returns {Promise<{month:string, settledCount:number, settledAmount:number}>}
 */
export function settleLastMonth() {
  return api.post('/account/settle-last-month').then((res) => res.data || {})
}
