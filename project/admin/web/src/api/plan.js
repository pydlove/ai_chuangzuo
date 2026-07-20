import request from '@/utils/request.js'

const BASE = '/api/v1/admin/plans'

/** 拉取所有套餐元数据（含停用）。 */
export function fetchPlans() {
  return request.get(BASE).then((res) => res.data || [])
}

/** 新增或更新套餐（按 planKey 区分）。 */
export function upsertPlan(data) {
  return request.post(BASE, data).then((res) => res.data)
}