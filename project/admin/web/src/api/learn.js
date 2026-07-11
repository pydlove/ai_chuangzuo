import request from '@/utils/request.js'

const BASE = '/api/v1/admin/learn'

// ---------- 分类 ----------
export function fetchCategoryTree() {
  return request.get(`${BASE}/category/tree`).then((res) => res.data || [])
}
export function createCategory(data) {
  return request.post(`${BASE}/category`, data).then((res) => res.data)
}
export function updateCategory(id, data) {
  return request.put(`${BASE}/category/${id}`, data)
}
export function deleteCategory(id) {
  return request.delete(`${BASE}/category/${id}`)
}
export function sortCategory(items) {
  return request.post(`${BASE}/category/sort`, { items })
}

// ---------- 文章 ----------
export function fetchArticlePage(params) {
  return request.get(`${BASE}/article/page`, { params }).then((res) => res.data)
}
export function fetchArticle(id) {
  return request.get(`${BASE}/article/${id}`).then((res) => res.data)
}
export function createArticle(data) {
  return request.post(`${BASE}/article`, data).then((res) => res.data)
}
export function updateArticle(id, data) {
  return request.put(`${BASE}/article/${id}`, data)
}
export function deleteArticle(id) {
  return request.delete(`${BASE}/article/${id}`)
}
export function publishArticle(id) {
  return request.post(`${BASE}/article/${id}/publish`)
}
export function unpublishArticle(id) {
  return request.post(`${BASE}/article/${id}/unpublish`)
}
export function moveArticle(id, categoryId) {
  return request.post(`${BASE}/article/${id}/move`, { categoryId })
}
export function sortArticle(items) {
  return request.post(`${BASE}/article/sort`, { items })
}
