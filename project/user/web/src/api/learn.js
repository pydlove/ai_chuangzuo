import request from '@/utils/request'

/**
 * 创作学院 - 分类树（仅包含有已发布文章的路径）。
 * @returns {Promise<{code:number, data:Array, message:string}>}
 */
export function fetchCategoryTree() {
  return request.get('/learn/category/tree')
}

/**
 * 创作学院 - 分类详情 + 已发布文章分页列表。
 * @param {number|string} id 分类 ID
 * @param {number} page
 * @param {number} size
 */
export function fetchCategoryDetail(id, page = 1, size = 50) {
  return request.get(`/learn/category/${id}`, { params: { page, size } })
}

/**
 * 创作学院 - 文章详情。
 * @param {number|string} id 文章 ID
 */
export function fetchArticle(id) {
  return request.get(`/learn/article/${id}`)
}
