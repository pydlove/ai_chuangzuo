import request from '@/utils/request'

export function getCoinLeaderboard(month) {
  return request.get('/leaderboards/coin', { params: { month } })
}

export function getIncomeLeaderboard(periodType, periodValue) {
  return request.get('/leaderboards/income', { params: { periodType, periodValue } })
}

export function submitIncomeSubmission(data) {
  return request.post('/leaderboards/income-submissions', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getMyIncomeSubmissions() {
  return request.get('/leaderboards/income-submissions/me')
}
