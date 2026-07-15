import request from '@/utils/request.js'

export function getOrderList(params = {}) {
  return request.get('/api/v1/admin/orders', { params }).then((res) => res.data)
}

export function getOrderDetail(id) {
  return request.get(`/api/v1/admin/orders/${id}`).then((res) => res.data)
}

export function markOrderPaid(id) {
  return request.post(`/api/v1/admin/orders/${id}/mark-paid`).then((res) => res.data)
}

export function refundOrder(id, data) {
  return request.post(`/api/v1/admin/orders/${id}/refund`, data).then((res) => res.data)
}

export function cancelOrder(id) {
  return request.post(`/api/v1/admin/orders/${id}/cancel`).then((res) => res.data)
}

export function adjustMembership(data) {
  return request.post('/api/v1/admin/membership/adjust', data).then((res) => res.data)
}

export function grantMembership(data) {
  return request.post('/api/v1/admin/membership/grant', data).then((res) => res.data)
}

export function getOrderStatsOverview() {
  return request.get('/api/v1/admin/orders/stats/overview').then((res) => res.data)
}

export function getOrderTrend(days = 7) {
  return request.get('/api/v1/admin/orders/stats/trend', { params: { days } }).then((res) => res.data)
}

export function getPlanDistribution() {
  return request.get('/api/v1/admin/orders/stats/plan-distribution').then((res) => res.data)
}
