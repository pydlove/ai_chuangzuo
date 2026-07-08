import request from '@/utils/request.js'

const SOURCE_TYPE_MAP = { 1: 'my', 2: 'learned' }
const STATUS_MAP = { 0: 'pending', 1: 'approved', 2: 'rejected' }

function normalize(row) {
  return {
    id: row.bizNo,
    name: row.styleName,
    sourceType: SOURCE_TYPE_MAP[row.sourceType] ?? 'my',
    creatorName: row.creatorName,
    prompt: row.prompt,
    scope: row.scope,
    status: STATUS_MAP[row.auditStatus] ?? 'pending',
    rejectReason: row.rejectReason,
    createdAt: row.createdAt
  }
}

export function listStyles(params = {}) {
  const { keyword = '', pageNum = 1, pageSize = 20, status } = params
  const query = { keyword, pageNum, pageSize }
  if (status !== undefined && status !== null && status !== '') {
    query.status = status
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