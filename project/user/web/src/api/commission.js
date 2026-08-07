import { api } from '@/api/auth'

export function listCommissionTasks(params = {}) {
  return api.get('/commission/tasks', { params }).then((res) => res.data || { records: [], total: 0 })
}

export function getCommissionTask(taskId) {
  return api.get(`/commission/tasks/${taskId}`).then((res) => res.data)
}

export function submitCommissionArticle(taskId, articleBizNo) {
  return api.post(`/commission/tasks/${taskId}/submissions`, { articleBizNo }).then((res) => res.data)
}

export function withdrawCommissionSubmission(submissionId) {
  return api.delete(`/commission/submissions/${submissionId}`)
}

export function getCommissionStats() {
  return api.get('/commission/stats').then((res) => res.data)
}

export function listMyCommissionSubmissions(params = {}) {
  return api.get('/commission/submissions/mine', { params }).then((res) => res.data || { records: [], total: 0 })
}
