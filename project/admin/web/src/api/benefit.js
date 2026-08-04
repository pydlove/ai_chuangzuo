import request from '@/utils/request.js'

const BASE = '/benefits'

export function fetchBenefits() {
  return request.get(BASE).then((res) => res.data || [])
}