import request from '@/utils/request.js'

export function listConfigs() {
  return request.get('/model-configs').then((res) => res.data)
}

export function getConfig(id) {
  return request.get(`/model-configs/${id}`).then((res) => res.data)
}

export function createConfig(data) {
  return request.post('/model-configs', data).then((res) => res.data)
}

export function updateConfig(id, data) {
  return request.put(`/model-configs/${id}`, data)
}

export function deleteConfig(id) {
  return request.delete(`/model-configs/${id}`)
}

export function fetchModels(data) {
  return request.post('/model-configs/actions/fetch-models', data).then((res) => res.data)
}

export function listProviderModels(providerType) {
  return request.get('/model-configs/provider-models', { params: { providerType } }).then((res) => res.data)
}

export function testConnection(data) {
  return request.post('/model-configs/actions/test-connection', data).then((res) => res.data)
}

export function toggleActive(id, data) {
  return request.post(`/model-configs/${id}/actions/toggle-active`, data)
}

export function chatTest(data) {
  return request.post('/model-configs/actions/chat-test', data).then((res) => res.data)
}
