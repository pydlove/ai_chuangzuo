import request from '@/utils/request.js'

const BASE = '/simulation/batches'

export function createBatch(data) {
  return request.post(BASE, data).then((res) => res.data)
}

export function createFreeCreateBatch(data) {
  return request.post(`${BASE}/free-create`, data).then((res) => res.data)
}

export function listBatches(params = {}) {
  return request.get(BASE, { params }).then((res) => res.data)
}

export function getBatch(id) {
  return request.get(`${BASE}/${id}`).then((res) => res.data)
}

export function listBatchLogs(id, params = {}) {
  return request.get(`${BASE}/${id}/logs`, { params }).then((res) => res.data)
}

export function cancelBatch(id) {
  return request.post(`${BASE}/${id}/cancel`).then((res) => res.data)
}

export function getStatsFilter() {
  return request.get('/simulation/stats-filter').then((res) => res.data)
}

export function updateStatsFilter(includeRobots) {
  return request.put('/simulation/stats-filter', { includeRobots }).then((res) => res.data)
}

// ---------- 素材库（昵称库/头像库） ----------
const LIBRARY = '/simulation/library'

export function listNicknames(params = {}) {
  return request.get(`${LIBRARY}/nicknames`, { params }).then((res) => res.data)
}

export function countNicknames() {
  return request.get(`${LIBRARY}/nicknames/count`).then((res) => res.data)
}

export function addNicknames(nicknames) {
  return request.post(`${LIBRARY}/nicknames`, { nicknames }).then((res) => res.data)
}

export function importNicknamesExcel(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`${LIBRARY}/nicknames/excel`, formData).then((res) => res.data)
}

export function deleteNickname(id) {
  return request.delete(`${LIBRARY}/nicknames/${id}`).then((res) => res.data)
}

export function listAvatars(params = {}) {
  return request.get(`${LIBRARY}/avatars`, { params }).then((res) => res.data)
}

export function countAvatars() {
  return request.get(`${LIBRARY}/avatars/count`).then((res) => res.data)
}

export function uploadAvatars(files) {
  const formData = new FormData()
  files.forEach((f) => formData.append('files', f))
  return request.post(`${LIBRARY}/avatars`, formData).then((res) => res.data)
}

export function deleteAvatar(id) {
  return request.delete(`${LIBRARY}/avatars/${id}`).then((res) => res.data)
}
