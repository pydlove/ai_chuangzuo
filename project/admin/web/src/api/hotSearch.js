import request from '@/utils/request.js'

export function listPlatforms() {
  return request.get('/api/v1/admin/hot-search/platforms').then((res) => res.data)
}
export function createPlatform(data) {
  return request.post('/api/v1/admin/hot-search/platforms', data)
}
export function updatePlatform(id, data) {
  return request.put(`/api/v1/admin/hot-search/platforms/${id}`, data)
}
export function deletePlatform(id) {
  return request.delete(`/api/v1/admin/hot-search/platforms/${id}`)
}

export function listDaily(params) {
  return request.get('/api/v1/admin/hot-search/daily', { params }).then((res) => res.data)
}
export function createDaily(data) {
  return request.post('/api/v1/admin/hot-search/daily', data)
}
export function updateDaily(id, data) {
  return request.put(`/api/v1/admin/hot-search/daily/${id}`, data)
}
export function deleteDaily(id) {
  return request.delete(`/api/v1/admin/hot-search/daily/${id}`)
}
export function recrawlDaily(id) {
  return request.post(`/api/v1/admin/hot-search/daily/${id}/re-crawl`).then((res) => res.data)
}

export function getConfig() {
  return request.get('/api/v1/admin/hot-search/config').then((res) => res.data)
}
export function saveConfig(data) {
  return request.put('/api/v1/admin/hot-search/config', data)
}
export function crawlNow() {
  return request.post('/api/v1/admin/hot-search/crawl').then((res) => res.data)
}
export function getLastRun() {
  return request.get('/api/v1/admin/hot-search/crawl/last-run').then((res) => res.data)
}
