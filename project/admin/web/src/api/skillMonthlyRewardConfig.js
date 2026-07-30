import request from '@/utils/request.js'

export const getSkillMonthlyRewardConfig = () =>
  request.get('/api/v1/admin/skill-market/monthly-reward-config').then((res) => res.data)

export const updateSkillMonthlyRewardConfig = (payload) =>
  request.put('/api/v1/admin/skill-market/monthly-reward-config', payload).then((res) => res.data)
