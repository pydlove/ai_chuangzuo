import request from '@/utils/request.js'

export function listAiPrompts(params) {
  return request.get('/ai-prompts', { params }).then((res) => res.data)
}

export function listAiPromptCategories() {
  return request.get('/ai-prompts/categories').then((res) => res.data)
}

export function getAiPrompt(id) {
  return request.get(`/ai-prompts/${id}`).then((res) => res.data)
}

export function createAiPrompt(data) {
  return request.post('/ai-prompts', data)
}

export function updateAiPrompt(id, data) {
  return request.put(`/ai-prompts/${id}`, data)
}

export function enableAiPrompt(id) {
  return request.post(`/ai-prompts/${id}/actions/enable`)
}

export function disableAiPrompt(id) {
  return request.post(`/ai-prompts/${id}/actions/disable`)
}
