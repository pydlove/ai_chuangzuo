import request from '@/utils/request.js'

export function getAuditLogs(params = {}) {
  return request.get('/audit-logs', { params }).then((res) => res.data)
}

export function getAuditLogConfig() {
  return request.get('/audit-logs/config').then((res) => res.data)
}

export function updateAuditLogConfig(data) {
  return request.put('/audit-logs/config', data).then((res) => res.data)
}
