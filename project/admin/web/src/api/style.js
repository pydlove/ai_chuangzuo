import request from '@/utils/request.js'

const SOURCE_TYPE_MAP = { 1: 'my', 2: 'learned' }
const STATUS_MAP = { 0: 'pending', 1: 'approved', 2: 'rejected' }

function normalize(row) {
  // 后端 StyleReviewVO 已做 int → string 转换；这里兜底兼容原始行。
  const sourceType = SOURCE_TYPE_MAP[row.sourceType] ?? row.sourceType ?? 'my'
  const status = STATUS_MAP[row.auditStatus] ?? row.status ?? 'pending'
  return {
    id: row.bizNo || row.id,
    name: row.styleName || row.name,
    sourceType,
    creatorName: row.creatorName,
    prompt: row.prompt,
    scope: row.scope,
    status,
    rejectReason: row.rejectReason,
    createdAt: row.createdAt
  }
}

export function listStyles(params = {}) {
  const { keyword = '', pageNum = 1, pageSize = 20, status, reviewed } = params
  const query = { keyword, pageNum, pageSize }
  if (status !== undefined && status !== null && status !== '') {
    query.status = status
  }
  if (reviewed !== undefined && reviewed !== null) {
    query.reviewed = reviewed
  }
  return request.get('/api/v1/admin/style-reviews', { params: query }).then((body) => {
    const data = body.data || {}
    const rows = data.records || data.list || []
    return {
      list: rows.map(normalize),
      total: data.total || 0
    }
  })
}

export function rejectStyle(id, reason) {
  return request.post(`/api/v1/admin/style-reviews/${id}/actions/reject`, { reason })
}

export function approveStyle(id) {
  return request.post(`/api/v1/admin/style-reviews/${id}/actions/approve`)
}

export function approveBatch(ids) {
  return request.post('/api/v1/admin/style-reviews/actions/batch-approve', { bizNos: ids }).then((body) => {
    return body.data || 0
  })
}