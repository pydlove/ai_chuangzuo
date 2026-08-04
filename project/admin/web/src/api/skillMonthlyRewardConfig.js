import request from '@/utils/request.js'

export const getSkillMonthlyRewardConfig = () =>
  request.get('/skill-market/monthly-reward-config').then((res) => res.data)

export const updateSkillMonthlyRewardConfig = (payload) =>
  request.put('/skill-market/monthly-reward-config', payload).then((res) => res.data)

export const getSkillPricePerUse = () =>
  request.get('/skill-market/price-per-use').then((res) => res.data)

export const updateSkillPricePerUse = (pricePerUse) =>
  request.put('/skill-market/price-per-use', { pricePerUse }).then((res) => res.data)
