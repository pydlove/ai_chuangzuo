import request from '@/utils/request.js'

// 账户明细
export function listAccounts(params) {
  return request.get('/api/v1/admin/accounts', { params }).then((res) => res.data)
}

export function getAccountDetail(userId) {
  return request.get(`/api/v1/admin/accounts/${userId}`).then((res) => res.data)
}

// 结算中心
export function getPendingSettlementSummary(month) {
  return request.get('/api/v1/admin/accounts/settlements/pending-summary', { params: { month } }).then((res) => res.data)
}

export function getPendingSettlementUsers(month) {
  return request.get('/api/v1/admin/accounts/settlements/pending-users', { params: { month } }).then((res) => res.data)
}

export function settleAccounts(data) {
  return request.post('/api/v1/admin/accounts/settlements/actions/settle', data)
}

// 自媒体审核
export function listIncomeSubmissions(params) {
  return request.get('/api/v1/admin/leaderboards/income-submissions', { params }).then((res) => res.data)
}

export function approveIncomeSubmission(id) {
  return request.post(`/api/v1/admin/leaderboards/income-submissions/${id}/approve`)
}

export function rejectIncomeSubmission(id, data) {
  return request.post(`/api/v1/admin/leaderboards/income-submissions/${id}/reject`, data)
}

// 榜单发奖
export function previewLeaderboardRewards(params) {
  return request.get('/api/v1/admin/leaderboards/rewards/preview', { params }).then((res) => res.data)
}

export function grantLeaderboardRewards(data) {
  return request.post('/api/v1/admin/leaderboards/rewards/actions/grant', data)
}

export function listLeaderboardRewards(params) {
  return request.get('/api/v1/admin/leaderboards/rewards', { params }).then((res) => res.data)
}
