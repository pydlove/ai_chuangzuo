import request from '@/utils/request.js'

export function listGlobalStyles(params = {}) {
  const { keyword = '', pageNum = 1, pageSize = 20, enableStatus } = params
  const query = { keyword, pageNum, pageSize }
  if (enableStatus !== undefined && enableStatus !== null && enableStatus !== '') {
    query.enableStatus = enableStatus
  }
  return request.get('/api/v1/admin/global-styles', { params: query }).then((body) => {
    const data = body.data || {}
    const rows = data.records || data.list || []
    return {
      list: rows,
      total: data.total || 0
    }
  })
}

export function createGlobalStyle(data) {
  return request.post('/api/v1/admin/global-styles', data).then((body) => body.data)
}

export function updateGlobalStyle(bizNo, data) {
  return request.put(`/api/v1/admin/global-styles/${bizNo}`, data)
}

export function deleteGlobalStyle(bizNo) {
  return request.delete(`/api/v1/admin/global-styles/${bizNo}`)
}