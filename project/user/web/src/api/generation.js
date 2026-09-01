import request from '@/utils/request'

export function submitGeneration(data) {
  return request.post('/generation-tasks', data).then((res) => res.data)
}

export function getGenerationTask(id, signal) {
  return request.get(`/generation-tasks/${id}`, signal ? { signal } : undefined).then((res) => res.data)
}

export function retryGenerationTask(id) {
  return request.post(`/generation-tasks/${id}/retry`).then((res) => res.data)
}

export function stopGenerationTask(id) {
  return request.post(`/generation-tasks/${id}/stop`).then((res) => res.data)
}

export function listGenerationTasks(params = {}) {
  return request.get('/generation-tasks', { params }).then((res) => res.data)
}
