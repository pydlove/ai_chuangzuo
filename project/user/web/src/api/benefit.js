import request from '@/utils/request'

export function getMyBenefits() {
  return request.get('/benefits/me')
}

export function checkBenefit(code) {
  return request.post(`/benefits/check/${code}`)
}

export function consumeBenefit(code) {
  return request.post(`/benefits/consume/${code}`)
}
