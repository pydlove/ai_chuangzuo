import request from '@/utils/request.js'

// ===== 模板 CRUD =====

export function listExportTemplates() {
  return request.get('/export-templates').then((res) => res.data)
}

export function getExportTemplate(id) {
  return request.get(`/export-templates/${id}`).then((res) => res.data)
}

export function createExportTemplate(data) {
  return request.post('/export-templates', data).then((res) => res.data)
}

export function updateExportTemplate(id, data) {
  return request.put(`/export-templates/${id}`, data).then((res) => res.data)
}

export function deleteExportTemplate(id) {
  return request.delete(`/export-templates/${id}`).then((res) => res.data)
}

// ===== 参数定义 CRUD =====

export function listExportTemplateParams() {
  return request.get('/export-templates/params').then((res) => res.data)
}

export function createExportTemplateParam(data) {
  return request.post('/export-templates/params', data).then((res) => res.data)
}

export function updateExportTemplateParam(id, data) {
  return request.put(`/export-templates/params/${id}`, data).then((res) => res.data)
}

export function deleteExportTemplateParam(id) {
  return request.delete(`/export-templates/params/${id}`).then((res) => res.data)
}