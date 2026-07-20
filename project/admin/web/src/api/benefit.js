import request from '@/utils/request.js'

const BASE = '/api/v1/admin/benefits'

export function fetchBenefits() {
  return request.get(BASE).then((res) => res.data || [])
}