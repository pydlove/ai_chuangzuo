import request from '@/utils/request.js'

export function listShareConfigs(params = {}) {
  return request.get('/share-config', { params }).then((res) => res.data)
}

export function getShareConfig(id) {
  return request.get(`/share-config/${id}`).then((res) => res.data)
}

export function createShareConfig(data) {
  return request.post('/share-config', data).then((res) => res.data)
}

export function updateShareConfig(id, data) {
  return request.put(`/share-config/${id}`, data).then((res) => res.data)
}

export function deleteShareConfig(id) {
  return request.delete(`/share-config/${id}`).then((res) => res.data)
}
