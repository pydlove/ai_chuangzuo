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
