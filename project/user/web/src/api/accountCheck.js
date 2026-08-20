import { api } from '@/api/auth'

const BASE = '/self-media/nickname'

/**
 * 检测账号昵称是否与自媒体定位契合。
 * @param {object} data
 * @param {string} data.nickname 待检测昵称
 * @param {string} [data.platform] 平台显示名
 * @param {string} [data.positioning] 自媒体定位摘要
 * @returns {Promise<{fit:boolean, reason:string, suggestions:string[]}>}
 */
export function checkNickname(data) {
  return api.post(`${BASE}/check`, data, { timeout: 90000 }).then((res) => res.data || {})
}

/**
 * 基于自媒体定位推荐账号昵称和简介。
 * @returns {Promise<{options:{nickname:string,bio:string}[]}>}
 */
export function recommendNickname() {
  return api.post(`${BASE}/recommend`, {}, { timeout: 90000 }).then((res) => res.data || {})
}

