import request from '@/utils/request.js'

export function getOrderList(params = {}) {
  return request.get('/orders', { params }).then((res) => res.data)
}

export function getOrderDetail(id) {
  return request.get(`/orders/${id}`).then((res) => res.data)
}

export function markOrderPaid(id) {
  return request.post(`/orders/${id}/mark-paid`).then((res) => res.data)
}

export function refundOrder(id, data) {
  return request.post(`/orders/${id}/refund`, data).then((res) => res.data)
}

export function cancelOrder(id) {
  return request.post(`/orders/${id}/cancel`).then((res) => res.data)
}

export function batchCancelOrder(ids) {
  return request.post('/orders/batch/cancel', { ids }).then((res) => res.data)
}

export function batchDeleteOrder(ids) {
  return request.post('/orders/batch/delete', { ids }).then((res) => res.data)
}

export function adjustMembership(data) {
  return request.post('/membership/adjust', data).then((res) => res.data)
}

export function grantMembership(data) {
  return request.post('/membership/grant', data).then((res) => res.data)
}

export function getOrderStatsOverview() {
  return request.get('/orders/stats/overview').then((res) => res.data)
}

export function getOrderTrend(days = 7) {
  return request.get('/orders/stats/trend', { params: { days } }).then((res) => res.data)
}

export function getPlanDistribution() {
  return request.get('/orders/stats/plan-distribution').then((res) => res.data)
}

export function getRenewalOverview() {
  return request.get('/orders/stats/renewal/overview').then((res) => res.data)
}

export function getRenewalTrend(days = 7) {
  return request.get('/orders/stats/renewal/trend', { params: { days } }).then((res) => res.data)
}

export function getRenewalDistribution() {
  return request.get('/orders/stats/renewal/distribution').then((res) => res.data)
}

export function getRenewalUserList(params = {}) {
  return request.get('/orders/stats/renewal/users', { params }).then((res) => res.data)
}

export function getRenewalPaidUsers(params = {}) {
  return request.get('/orders/stats/renewal/paid-users', { params }).then((res) => res.data)
}

export function getRenewalOrderList(params = {}) {
  return request.get('/orders/stats/renewal/orders', { params }).then((res) => res.data)
}
