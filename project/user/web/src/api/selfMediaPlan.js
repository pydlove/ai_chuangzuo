import { api } from '@/api/auth'

const BASE = '/self-media-plans'

/**
 * 获取当前用户的自媒体方案。
 * @returns {Promise<{code:number, data:object|null, msg:string}>}
 */
export function fetchCurrentPlan() {
  return api.get(`${BASE}/current`)
}

/**
 * AI 推荐平台。
 * @param {object} data
 * @returns {Promise<{code:number, data:{platformKey:string, platformName:string, reason:string}, msg:string}>}
 */
export function recommendPlatform(data) {
  return api.post(`${BASE}/actions/recommend-platform`, data, { timeout: 90000 })
}

/**
 * AI 推荐目标。
 * @param {object} data
 * @returns {Promise<{code:number, data:Array<{key:string,name:string,description:string}>, msg:string}>}
 */
export function recommendGoals(data) {
  return api.post(`${BASE}/actions/recommend-goals`, data, { timeout: 90000 })
}

/**
 * AI 推荐赛道。
 * @param {object} data
 * @returns {Promise<{code:number, data:Array, msg:string}>}
 */
export function recommendNiches(data) {
  return api.post(`${BASE}/actions/recommend-niches`, data, { timeout: 90000 })
}

/**
 * AI 推荐人设与默认内容支柱。
 * @param {object} data
 * @returns {Promise<{code:number, data:{personas:Array, defaultPillars:Array}, msg:string}>}
 */
export function recommendPersonas(data) {
  return api.post(`${BASE}/actions/recommend-personas`, data, { timeout: 90000 })
}

/**
 * 保存自媒体方案。
 * @param {object} data
 * @returns {Promise<{code:number, data:object, msg:string}>}
 */
export function savePlan(data) {
  return api.post(BASE, data)
}
