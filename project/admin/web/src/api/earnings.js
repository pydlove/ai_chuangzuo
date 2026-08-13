import request from '@/utils/request.js'

// 账户明细
export function listAccounts(params) {
  return request.get('/accounts', { params }).then((res) => res.data)
}

export function getAccountDetail(userId) {
  return request.get(`/accounts/${userId}`).then((res) => res.data)
}

export function listUserCoinRecords(userId, params) {
  return request.get(`/accounts/${userId}/coin-records`, { params }).then((res) => res.data)
}

export function listUserEarningsRecords(userId, params) {
  return request.get(`/accounts/${userId}/earnings-records`, { params }).then((res) => res.data)
}

export function listUserRewardRecords(userId, params) {
  return request.get(`/accounts/${userId}/reward-records`, { params }).then((res) => res.data)
}
