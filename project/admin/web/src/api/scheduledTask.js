import request from '@/utils/request.js'

export function getScheduledTasks(params = {}) {
  return request.get('/scheduled-tasks', { params }).then((res) => res.data)
}

export function triggerScheduledTask(id) {
  return request.post(`/scheduled-tasks/${id}/actions/trigger`).then((res) => res.data)
}

export function getScheduledTaskLogs(id, limit = 5) {
  return request.get(`/scheduled-tasks/${id}/logs`, { params: { limit } }).then((res) => res.data)
}
