import request from '@/utils/request.js'

export function listConfigs() {
  return request.get('/api/v1/admin/model-configs').then((res) => res.data)
}

export function getConfig(providerType) {
  return request.get(`/api/v1/admin/model-configs/${providerType}`).then((res) => res.data)
}

export function saveConfig(providerType, data) {
  return request.put(`/api/v1/admin/model-configs/${providerType}`, data)
}

export function deleteConfig(providerType) {
  return request.delete(`/api/v1/admin/model-configs/${providerType}`)
}

export function fetchModels(providerType, data) {
  return request.post(`/api/v1/admin/model-configs/${providerType}/actions/fetch-models`, data).then((res) => res.data)
}

export function testConnection(providerType, data) {
  return request.post(`/api/v1/admin/model-configs/${providerType}/actions/test-connection`, data).then((res) => res.data)
}

export function toggleActive(providerType, data) {
  return request.post(`/api/v1/admin/model-configs/${providerType}/actions/toggle-active`, data)
}
