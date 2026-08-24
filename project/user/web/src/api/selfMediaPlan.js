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
 * 获取或生成平台问题。
 * @param {string} platformKey
 * @returns {Promise<{code:number, data:Array<{key:string,text:string,options:Array<{key:string,label:string}>,isRequired:boolean,sortOrder:number}>, msg:string}>}
 */
export function fetchPlatformQuestions(platformKey) {
  return api.get(`${BASE}/platform-questions`, { params: { platformKey }, timeout: 60000 })
}

/**
 * AI 推荐赛道。
 * @param {object} data
 * @param {string} data.platformKey
 * @param {Array<{questionKey:string,answer:string}>} data.answers
 * @returns {Promise<{code:number, data:Array, msg:string}>}
 */
export function recommendNiches(data) {
  return api.post(`${BASE}/actions/recommend-niches`, data, { timeout: 90000 })
}

/**
 * AI 推荐人设与默认内容支柱。
 * @param {object} data
 * @param {string} data.platformKey
 * @param {string} data.nicheKey
 * @param {Array<{questionKey:string,answer:string}>} data.answers
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

/**
 * 根据自媒体运营方案生成发布计划（主平台规律时段 + 冷启动策略 + 一文多发）。
 * @param {{articleTitle:string, mainPlatform:string}} data
 * @returns {Promise<{code:number, data:{mainPlatform:{platform:string,publishTime:string,reason:string}, coldStart:{immediateActions:string[],duration:string,sharingTips:string}, reposts:{platform:string,publishTime:string,title:string,tags:string[],imageSuggestions:string}[]}, msg:string}>}
 */
export function generatePublishPlan(data) {
  return api.post(`${BASE}/actions/publish-plan`, data, { timeout: 90000 })
}
