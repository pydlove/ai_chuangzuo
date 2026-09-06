import request from '@/utils/request'

export function getCurrentCampaign() {
  return request({ url: '/lottery/campaigns/current', method: 'get' })
}

export function getChances(campaignId) {
  return request({ url: '/lottery/chances', method: 'get', params: { campaignId } })
}

export function draw(campaignId) {
  return request({ url: '/lottery/draw', method: 'post', data: { campaignId } })
}

export function redeem(code) {
  return request({ url: '/lottery/redeem', method: 'post', data: { code } })
}

export function getDisplayWinnersPaged(campaignId, page = 1, pageSize = 20) {
  return request({ url: '/lottery/display-winners', method: 'get', params: { campaignId, page, pageSize } })
}

export function getGrandWinners(campaignId) {
  return request({ url: '/lottery/display-winners/grand', method: 'get', params: { campaignId } })
}

export function getMyCodes() {
  return request({ url: '/lottery/my-codes', method: 'get' })
}

export function getMyCoupons() {
  return request({ url: '/coupons', method: 'get' })
}
