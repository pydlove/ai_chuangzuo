import request from '@/utils/request.js'

/** 拉取任务列表（按 status 过滤）。 */
export const listGenerationTasks = (params) =>
  request.get('/api/v1/admin/generation/tasks', { params }).then((res) => res.data)

/** 手动停止任务：QUEUED / PROCESSING → FAILED。 */
export const stopGenerationTask = (id) =>
  request.post(`/api/v1/admin/generation/tasks/${id}/stop`).then((res) => res.data)

/** 手动标记失败。 */
export const markGenerationTaskFailed = (id, reason) =>
  request.post(`/api/v1/admin/generation/tasks/${id}/mark-failed`, { reason }).then((res) => res.data)

/**
 * 查某任务的 AI 调用日志（按 stage 分组）+ 任务当前 status。
 * 返回 { taskStatus, grouped }，其中 grouped 是 { "2": [log, ...], ... }，key 为 stageIndex。
 * taskStatus=2（已完成）时前端应停止轮询。仅 AI 阶段（2,3,4,6,7,8,9,11）有日志。
 */
export const getGenerationCallLogsGrouped = (taskId) =>
  request.get(`/api/v1/admin/generation/call-logs/by-task/${taskId}/grouped`).then((res) => res.data)
