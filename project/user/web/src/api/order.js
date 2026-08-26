import { api } from '@/api/auth'

/**
 * 查询我的订单列表。
 * @param {{status?:number, page?:number, pageSize?:number}} params
 * @returns {Promise<{list:Array, total:number, page:number, pageSize:number}>}
 */
export function getMyOrders(params = {}) {
  return api.get('/orders', { params }).then((res) => res.data || { list: [], total: 0 })
}
