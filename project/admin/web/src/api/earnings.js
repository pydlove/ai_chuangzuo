import request from '@/utils/request.js'

// 账户明细
export function listAccounts(params) {
  return request.get('/accounts', { params }).then((res) => res.data)
}

export function getAccountDetail(userId) {
  return request.get(`/accounts/${userId}`).then((res) => res.data)
}
