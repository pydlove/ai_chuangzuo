import request from '@/utils/request.js'

/** 拉取任务列表（按 status 过滤）。 */
export const listGenerationTasks = (params) =>
  request.get('/api/v1/admin/generation/tasks', { params }).then((res) => res.data)

/** 手动重试：把任务回滚为 queued。 */
export const manualRetryGenerationTask = (id) =>
  request.post(`/api/v1/admin/generation/tasks/${id}/retry`).then((res) => res.data)

/** 强制释放 lease：processing → queued。 */
export const releaseGenerationTaskLease = (id) =>
  request.post(`/api/v1/admin/generation/tasks/${id}/release-lease`).then((res) => res.data)

/** 手动标记失败。 */
export const markGenerationTaskFailed = (id, reason) =>
  request.post(`/api/v1/admin/generation/tasks/${id}/mark-failed`, { reason }).then((res) => res.data)

/**
 * 查某任务的 AI 调用日志（按 stage 分组）。
 * 返回 { "2": [log, ...], "3": [...], ... }，key 为 stageIndex（JSON 里是字符串）。
 * 仅 AI 阶段（2,3,4,6,7,8,9,11）有日志；规则/直通阶段（1,5,10,12）不产生。
 */
export const getGenerationCallLogsGrouped = (taskId) =>
  request.get(`/api/v1/admin/generation/call-logs/by-task/${taskId}/grouped`).then((res) => res.data)
