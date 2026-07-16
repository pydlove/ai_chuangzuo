import { api } from '@/api/auth'

/**
 * 分页查询我的作品。
 * @param {{keyword?:string, page?:number, pageSize?:number}} params
 * @returns {Promise<{list:Array, total:number, page:number, pageSize:number}>}
 */
export function listArticles(params = {}) {
  return api.get('/articles', { params }).then((res) => res.data || { list: [], total: 0 })
}

/**
 * 获取作品详情。
 * @param {string} bizNo
 */
export function getArticle(bizNo) {
  return api.get(`/articles/${bizNo}`).then((res) => res.data || null)
}

/**
 * 保存作品（生成完成时调用）。
 * @param {{title:string, body:string, styleOverrides?:string, platform?:string, style?:string, template?:string, wordCount?:number, completedAt?:string}} payload
 * @returns {Promise<string>} bizNo
 */
export function saveArticle(payload) {
  return api.post('/articles', payload).then((res) => res.data)
}

/**
 * 修改作品。
 * @param {string} bizNo
 * @param {{title?:string, body?:string, styleOverrides?:string}} payload
 */
export function updateArticle(bizNo, payload) {
  return api.put(`/articles/${bizNo}`, payload)
}

/**
 * 删除作品。
 * @param {string} bizNo
 */
export function deleteArticle(bizNo) {
  return api.delete(`/articles/${bizNo}`)
}

/**
 * AI 标题优化：首次点击调用大模型生成，之后返回首次缓存结果。
 * @param {string} bizNo
 * @returns {Promise<{titles:Object<string,string[]>, cached:boolean}>}
 */
export function optimizeTitles(bizNo) {
  return api.post(`/articles/${bizNo}/title-optimize`).then((res) => res.data || { titles: {}, cached: true })
}

/**
 * 查询当前用户本月已生成作品数。
 * @returns {Promise<number>}
 */
export function getMonthlyCount() {
  return api.get('/articles/monthly-count').then((res) => res.data)
}