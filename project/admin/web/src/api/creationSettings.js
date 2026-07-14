import request from '@/utils/request.js'

/** 拉取当前创作运行时配置。 */
export const getGenerationConfig = () =>
  request.get('/api/v1/admin/generation/config').then((res) => res.data)

/** 更新创作运行时配置。 */
export const updateGenerationConfig = (payload) =>
  request.put('/api/v1/admin/generation/config', payload).then((res) => res.data)
