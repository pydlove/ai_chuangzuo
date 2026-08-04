import request from '@/utils/request.js'

export function listPlatforms() {
  return request.get('/hot-search/platforms').then((res) => res.data)
}
export function createPlatform(data) {
  return request.post('/hot-search/platforms', data)
}
export function updatePlatform(id, data) {
  return request.put(`/hot-search/platforms/${id}`, data)
}
export function deletePlatform(id) {
  return request.delete(`/hot-search/platforms/${id}`)
}

export function listDaily(params) {
  return request.get('/hot-search/daily', { params }).then((res) => res.data)
}
export function createDaily(data) {
  return request.post('/hot-search/daily', data)
}
export function updateDaily(id, data) {
  return request.put(`/hot-search/daily/${id}`, data)
}
export function deleteDaily(id) {
  return request.delete(`/hot-search/daily/${id}`)
}
export function recrawlDaily(id) {
  return request.post(`/hot-search/daily/${id}/re-crawl`).then((res) => res.data)
}

export function getConfig() {
  return request.get('/hot-search/config').then((res) => res.data)
}
export function saveConfig(data) {
  return request.put('/hot-search/config', data)
}
export function crawlNow() {
  return request.post('/hot-search/crawl').then((res) => res.data)
}
export function getLastRun() {
  return request.get('/hot-search/crawl/last-run').then((res) => res.data)
}
export function listCrawlLogs(params) {
  return request.get('/hot-search/crawl/logs', { params }).then((res) => res.data)
}
