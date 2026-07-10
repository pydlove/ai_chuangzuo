import request from '@/utils/request.js'

export function listTemplates(params = {}) {
  return request.get('/api/v1/admin/prompt-templates', { params }).then((res) => res.data)
}

export function getTemplate(id) {
  return request.get(`/api/v1/admin/prompt-templates/${id}`).then((res) => res.data)
}

export function createTemplate(data) {
  return request.post('/api/v1/admin/prompt-templates', data).then((res) => res.data)
}

export function updateTemplate(id, data) {
  return request.put(`/api/v1/admin/prompt-templates/${id}`, data).then((res) => res.data)
}

export function enableTemplate(id) {
  return request.post(`/api/v1/admin/prompt-templates/${id}/enable`)
}

export function disableTemplate(id) {
  return request.post(`/api/v1/admin/prompt-templates/${id}/disable`)
}

export function deleteTemplate(id) {
  return request.delete(`/api/v1/admin/prompt-templates/${id}`)
}

/** 老模板初始化 12 阶段默认值（返回插入的 stage 数量）。 */
export function initTemplateStages(id) {
  return request.post(`/api/v1/admin/prompt-templates/${id}/init-stages`).then((res) => res.data)
}

// ===== 阶段 2：发布 / 下线 / 克隆 / 版本 =====

/**
 * 发布模板：把当前 12 阶段配置快照为新版本号，置 status=PUBLISHED。
 * @returns {Promise<{data: number}>} data = 新版本号
 */
export function publishTemplate(id, changeNote) {
  return request
    .post(`/api/v1/admin/prompt-templates/${id}/actions/publish`, { changeNote })
    .then((res) => res.data)
}

/** 下线模板（仅 PUBLISHED 可下线）。 */
export function offlineTemplate(id) {
  return request.post(`/api/v1/admin/prompt-templates/${id}/actions/offline`)
}

/**
 * 克隆模板。
 * @param {object} body { name, remark?, sourceVersion? }
 * @returns {Promise<{data: number}>} data = 新模板 id
 */
export function cloneTemplate(id, body) {
  return request
    .post(`/api/v1/admin/prompt-templates/${id}/actions/clone`, body)
    .then((res) => res.data)
}

/** 取模板的全部版本快照摘要。 */
export function listTemplateVersions(id) {
  return request.get(`/api/v1/admin/prompt-templates/${id}/versions`).then((res) => res.data)
}
