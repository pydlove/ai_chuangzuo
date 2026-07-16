import { api } from '@/api/auth'

/**
 * 获取当前登录用户的风格列表。
 * @param {number} sourceType 来源类型：1-自定义（默认），2-学习
 * @returns {Promise<{code:number, data:Array<{bizNo:string, styleName:string, prompt:string, scope:string, sourceType:number, useCount:number, createdAt:string, updatedAt:string}>}>}
 */
export function getMyStyles(sourceType = 1) {
  return api.get('/styles', { params: { sourceType } })
}

/**
 * 创建自定义风格。
 * @param {{styleName:string, prompt:string, scope:string}} data
 */
export function createStyle(data) {
  return api.post('/styles', data)
}

/**
 * 修改风格。
 * @param {string} bizNo
 * @param {{styleName:string, prompt:string, scope:string}} data
 */
export function updateStyle(bizNo, data) {
  return api.put(`/styles/${bizNo}`, data)
}

/**
 * 删除风格。
 * @param {string} bizNo
 */
export function deleteStyle(bizNo) {
  return api.delete(`/styles/${bizNo}`)
}

/**
 * 获取系统预设风格（启用中的 source_type=3）。
 * @param {string} [keyword]
 */
export function getSystemStyles(keyword = '') {
  const params = keyword ? { keyword } : {}
  return api.get('/styles/system-styles', { params })
}

/**
 * AI 分析参考文章风格。
 * 注意：AI 分析约 10-30 秒，axios 实例默认 timeout 10s 必然超时，必须单独传 90s。
 * @param {string} text 参考文章正文（200-3000 字）
 * @returns {Promise<{code:number, data:{excerpt1:string, excerpt2:string, prompt:string}}>}
 */
export function analyzeStyle(text) {
  return api.post('/styles/analyze', { text }, { timeout: 90000 })
}
