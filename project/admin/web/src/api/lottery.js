import request from '@/utils/request'

export function listCampaigns(params) {
  return request({ url: '/lottery/campaigns', method: 'get', params })
}

export function getCampaign(id) {
  return request({ url: `/lottery/campaigns/${id}`, method: 'get' })
}

export function saveCampaign(data) {
  return request({ url: '/lottery/campaigns', method: 'post', data })
}

export function openCampaign(id) {
  return request({ url: `/lottery/campaigns/${id}/open`, method: 'post' })
}

export function closeCampaign(id) {
  return request({ url: `/lottery/campaigns/${id}/close`, method: 'post' })
}

export function cloneCampaign(id, data) {
  return request({ url: `/lottery/campaigns/${id}/clone`, method: 'post', data })
}

export function deleteCampaign(id) {
  return request({ url: `/lottery/campaigns/${id}`, method: 'delete' })
}

export function listTiers(campaignId) {
  return request({ url: `/lottery/campaigns/${campaignId}/tiers`, method: 'get' })
}

export function saveTier(campaignId, data) {
  return request({ url: `/lottery/campaigns/${campaignId}/tiers`, method: 'post', data })
}

export function deleteTier(campaignId, tierId) {
  return request({ url: `/lottery/campaigns/${campaignId}/tiers/${tierId}`, method: 'delete' })
}

export function listRedemptionCodes(params) {
  return request({ url: '/lottery/redemption-codes', method: 'get', params })
}

export function listDrawRecords(params) {
  return request({ url: '/lottery/draw-records', method: 'get', params })
}

export function resetDrawChance(data) {
  return request({ url: '/lottery/draw-chances/reset', method: 'post', params: data })
}

export function listDisplayWinners(campaignId) {
  return request({ url: '/lottery/display-winners', method: 'get', params: { campaignId } })
}

export function saveDisplayWinner(data) {
  return request({ url: '/lottery/display-winners', method: 'post', data })
}

export function toggleDisplayWinner(id, status) {
  return request({ url: `/lottery/display-winners/${id}/toggle`, method: 'post', params: { status } })
}

export function deleteDisplayWinner(id) {
  return request({ url: `/lottery/display-winners/${id}`, method: 'delete' })
}
