import request from '@/utils/request.js'

const BASE = '/api/v1/admin/commission/tasks'

export function fetchCommissionTasks(params = {}) {
  return request.get(BASE, { params }).then((res) => res.data)
}
export function fetchCommissionTask(id) {
  return request.get(`${BASE}/${id}`).then((res) => res.data)
}
export function createCommissionTask(data) {
  return request.post(BASE, data).then((res) => res.data)
}
export function updateCommissionTask(id, data) {
  return request.put(`${BASE}/${id}`, data).then((res) => res.data)
}
export function closeCommissionTask(id) {
  return request.post(`${BASE}/${id}/close`).then((res) => res.data)
}
export function adoptCommissionSubmissions(id, submissionIds) {
  return request.post(`${BASE}/${id}/adopt`, { submissionIds }).then((res) => res.data)
}
export function createCommissionSubmission(id, data) {
  return request.post(`${BASE}/${id}/submissions`, data).then((res) => res.data)
}
export function createCommissionSubmissionBatch(id, data) {
  return request.post(`${BASE}/${id}/submissions/batch`, data).then((res) => res.data)
}
