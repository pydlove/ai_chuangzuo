import request from '@/utils/request.js'

export function getSubmissions(params) {
  return request.get('/api/v1/admin/leaderboards/income-submissions', { params }).then((res) => res.data)
}

export function approveSubmission(id) {
  return request.post(`/api/v1/admin/leaderboards/income-submissions/${id}/approve`)
}

export function rejectSubmission(id, reason) {
  return request.post(`/api/v1/admin/leaderboards/income-submissions/${id}/reject`, { reason })
}

export function previewTop10(params) {
  return request.get('/api/v1/admin/leaderboards/rewards/preview', { params }).then((res) => res.data)
}

export function grantRewards(data) {
  return request.post('/api/v1/admin/leaderboards/rewards/actions/grant', data).then((res) => res.data)
}

export function getRewards(params) {
  return request.get('/api/v1/admin/leaderboards/rewards', { params }).then((res) => res.data)
}
