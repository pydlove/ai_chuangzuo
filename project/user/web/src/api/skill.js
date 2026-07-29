import { api } from '@/api/auth'

/**
 * 获取当前登录用户的 skills 列表。
 * @param {number} sourceType 来源类型：1-自定义（默认），2-学习
 * @returns {Promise<{code:number, data:Array<{bizNo:string, skillName:string, prompt:string, scope:string, sourceType:number, useCount:number, createdAt:string, updatedAt:string}>}>}
 */
export function getMySkills(sourceType = 1) {
  return api.get('/skills', { params: { sourceType } })
}

/**
 * 创建自定义 skills。
 * @param {{skillName:string, prompt:string, scope:string}} data
 */
export function createSkill(data) {
  return api.post('/skills', data)
}

/**
 * 修改 skills。
 * @param {string} bizNo
 * @param {{skillName:string, prompt:string, scope:string}} data
 */
export function updateSkill(bizNo, data) {
  return api.put(`/skills/${bizNo}`, data)
}

/**
 * 删除 skills。
 * @param {string} bizNo
 */
export function deleteSkill(bizNo) {
  return api.delete(`/skills/${bizNo}`)
}

/**
 * 获取系统预设 skills（启用中的 source_type=3）。
 * @param {string} [keyword]
 */
export function getSystemSkills(keyword = '') {
  const params = keyword ? { keyword } : {}
  return api.get('/skills/system-skills', { params })
}

/**
 * AI 分析参考文章 skills。
 * 注意：AI 分析约 10-30 秒，axios 实例默认 timeout 10s 必然超时，必须单独传 90s。
 * @param {string} text 参考文章正文（200-3000 字）
 * @returns {Promise<{code:number, data:{excerpt1:string, excerpt2:string, prompt:string}}>}
 */
export function analyzeSkill(text) {
  return api.post('/skills/analyze', { text }, { timeout: 90000 })
}
