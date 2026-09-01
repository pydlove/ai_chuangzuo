import request from '@/utils/request.js'

/** 获取支付配置。 */
export const getPaymentConfig = () =>
  request.get('/settings/payment-config').then((res) => res.data)

/** 更新支付配置。 */
export const updatePaymentConfig = (payload) =>
  request.put('/settings/payment-config', payload).then((res) => res.data)
