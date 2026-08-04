import request from '@/utils/request.js'

// 自媒体收入榜功能暂时隐藏
// export function getSubmissions(params) {
//   return request.get('/leaderboards/income-submissions', { params }).then((res) => res.data)
// }

// export function approveSubmission(id) {
//   return request.post(`/leaderboards/income-submissions/${id}/approve`)
// }

// export function rejectSubmission(id, reason) {
//   return request.post(`/leaderboards/income-submissions/${id}/reject`, { reason })
// }

export function previewTop10(params) {
  return request.get('/leaderboards/rewards/preview', { params }).then((res) => res.data)
}

export function grantRewards(data) {
  return request.post('/leaderboards/rewards/actions/grant', data).then((res) => res.data)
}

export function getRewards(params) {
  return request.get('/leaderboards/rewards', { params }).then((res) => res.data)
}
