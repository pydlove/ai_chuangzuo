import request from '@/utils/request.js'

/** 标题分页列表：keyword + page/pageSize。 */
export const listTopicTitles = (params) =>
  request.get('/topic-titles', { params }).then((res) => res.data)

/** 提交 AI 批量生成标题任务，立即返回 taskId（异步，后台 worker 执行）。 */
export const submitTopicTitleTask = (data) =>
  request.post('/topic-titles/generate', data).then((res) => res.data)

/** 查询任务状态：status(0/1/2/3) / generatedCount / failedReason。 */
export const getTopicTitleTask = (taskId) =>
  request.get(`/topic-titles/tasks/${taskId}`).then((res) => res.data)

/** 逻辑删除标题。 */
export const deleteTopicTitle = (id) =>
  request.delete(`/topic-titles/${id}`).then((res) => res.data)
