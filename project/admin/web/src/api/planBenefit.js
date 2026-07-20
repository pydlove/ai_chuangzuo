import request from '@/utils/request.js'

const BASE = '/api/v1/admin/plan-benefits'

/** 拉所有套餐 × 权益矩阵值。 */
export function fetchPlanBenefits() {
  return request.get(BASE).then((res) => res.data || [])
}

/** 单条 upsert：按 planKey+benefitCode 唯一。 */
export function upsertPlanBenefit(data) {
  return request.post(BASE, data).then((res) => res.data)
}