import { api } from '@/api/auth'

/**
 * 分页获取当前登录用户的 skills 列表。
 * @param {number} sourceType 来源类型：1-自定义（默认），2-学习
 * @param {string} [keyword] 搜索关键词，匹配名称/适用范围/提示词/描述
 * @param {number} [page] 页码，默认 1
 * @param {number} [pageSize] 每页条数，默认 999（不传则返回完整列表保持兼容）
 * @returns {Promise<{code:number, data:{records:Array<...>, total:number, current:number, size:number}}>}
 */
export function getMySkills(sourceType = 1, keyword = '', page = 1, pageSize = 999) {
  const params = { sourceType, page, pageSize }
  if (keyword && keyword.trim()) {
    params.keyword = keyword.trim()
  }
  return api.get('/skills', { params })
}

/**
 * 创建自定义 skills。
 * @param {{skillName:string, prompt:string, scope:string, description?:string, promptExtra?:string|null}} data
 */
export function createSkill(data) {
  return api.post('/skills', data)
}

/**
 * 修改 skills。
 * @param {string} bizNo
 * @param {{skillName:string, prompt:string, scope:string, description?:string, promptExtra?:string|null}} data
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
 * 获取系统预设 skills（启用中的 source_type=3），支持分页。
 * @param {string} [keyword]
 * @param {number} [page]
 * @param {number} [pageSize]
 */
export function getSystemSkills(keyword = '', page = 1, pageSize = 999) {
  const params = { page, pageSize }
  if (keyword && keyword.trim()) {
    params.keyword = keyword.trim()
  }
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
