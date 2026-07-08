import { api } from '@/api/auth'

/**
 * 分页查询我的草稿。
 * @param {{keyword?:string, page?:number, pageSize?:number}} params
 * @returns {Promise<{list:Array, total:number, page:number, pageSize:number}>}
 */
export function listDrafts(params = {}) {
  return api.get('/drafts', { params }).then((res) => res.data || { list: [], total: 0 })
}

/**
 * 获取草稿详情。
 * @param {string} bizNo
 */
export function getDraft(bizNo) {
  return api.get(`/drafts/${bizNo}`).then((res) => res.data || null)
}

/**
 * 保存草稿。
 * @param {{customTitle?:string, customRequirement?:string, platform?:string, wordCount?:number, style?:string, template?:string}} payload
 * @returns {Promise<string>} bizNo
 */
export function saveDraft(payload) {
  return api.post('/drafts', payload).then((res) => res.data)
}

/**
 * 修改草稿。
 * @param {string} bizNo
 * @param {{customTitle?:string, customRequirement?:string, platform?:string, wordCount?:number, style?:string, template?:string, savedAt?:string}} payload
 */
export function updateDraft(bizNo, payload) {
  return api.put(`/drafts/${bizNo}`, payload)
}

/**
 * 删除草稿。
 * @param {string} bizNo
 */
export function deleteDraft(bizNo) {
  return api.delete(`/drafts/${bizNo}`)
}