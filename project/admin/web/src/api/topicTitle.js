import request from '@/utils/request.js'

/** 标题分页列表：keyword + page/pageSize。 */
export const listTopicTitles = (params) =>
  request.get('/api/v1/admin/topic-titles', { params }).then((res) => res.data)

/** AI 批量生成标题入库：{count, direction} → {generated}。同步调 AI，放宽超时到 5 分钟。 */
export const generateTopicTitles = (data) =>
  request.post('/api/v1/admin/topic-titles/generate', data, { timeout: 300000 }).then((res) => res.data)

/** 逻辑删除标题。 */
export const deleteTopicTitle = (id) =>
  request.delete(`/api/v1/admin/topic-titles/${id}`).then((res) => res.data)
