import request from '@/utils/request.js'

/** 拉取当前创作运行时配置。 */
export const getGenerationConfig = () =>
  request.get('/generation/config').then((res) => res.data)

/** 更新创作运行时配置。 */
export const updateGenerationConfig = (payload) =>
  request.put('/generation/config', payload).then((res) => res.data)
