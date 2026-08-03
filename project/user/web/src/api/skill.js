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
 * 将当前用户的 skill 发布到提示词市场（进入待审核状态）。
 * @param {string} bizNo
 */
export function publishSkill(bizNo) {
  return api.post(`/skills/${bizNo}/publish`)
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
 * @param {string} text 参考文章正文（200-1000 字）
 * @returns {Promise<{code:number, data:{excerpt1:string, excerpt2:string, prompt:string}}>}
 */
export function analyzeSkill(text) {
  return api.post('/skills/analyze', { text }, { timeout: 90000 })
}

/**
 * 预扣学习提示词额度。
 * @returns {Promise<{code:number, data:{allowed:boolean, used:number, preUsed:number, remaining:number}}>}
 */
export function preConsumeAnalyzeQuota() {
  return api.post('/skills/analyze/pre-consume', {}, { timeout: 10000 })
}

/**
 * 确认扣减学习提示词额度（用户保存结果时调用）。
 * @returns {Promise<{code:number}>}
 */
export function confirmAnalyzeConsume() {
  return api.post('/skills/analyze/confirm', {}, { timeout: 10000 })
}

/**
 * 释放学习提示词预扣额度（用户取消或关闭弹框时调用）。
 * @returns {Promise<{code:number}>}
 */
export function cancelAnalyzeConsume() {
  return api.post('/skills/analyze/cancel', {}, { timeout: 10000 })
}
