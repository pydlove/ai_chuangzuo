import request from '@/utils/request'

export function getPaymentConfig() {
  return request.get('/payment/config')
}

export function getOrderStatus(orderNo) {
  return request.get(`/orders/${orderNo}/status`)
}
