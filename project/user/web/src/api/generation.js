import request from '@/utils/request'

export function submitGeneration(data) {
  return request.post('/generation-tasks', data).then((res) => res.data)
}

export function getGenerationTask(id) {
  return request.get(`/generation-tasks/${id}`).then((res) => res.data)
}

export function retryGenerationTask(id) {
  return request.post(`/generation-tasks/${id}/retry`).then((res) => res.data)
}

export function listGenerationTasks(params = {}) {
  return request.get('/generation-tasks', { params }).then((res) => res.data)
}
