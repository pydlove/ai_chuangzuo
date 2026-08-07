import request from '@/utils/request.js'

export function pageWithdrawals(params = {}) {
  return request.get('/withdrawals', { params }).then((body) => {
    const data = body.data || {}
    return {
      list: data.list || [],
      total: data.total || 0,
      page: params.page || 1,
      size: params.size || 20
    }
  })
}

export function approveWithdraw(bizNo) {
  return request.post(`/withdrawals/${bizNo}/approve`).then((body) => body.data)
}

export function rejectWithdraw(bizNo, remark) {
  return request.post(`/withdrawals/${bizNo}/reject`, { remark }).then((body) => body.data)
}
