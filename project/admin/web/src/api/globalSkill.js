import request from '@/utils/request.js'

export function listGlobalSkills(params = {}) {
  const { keyword = '', pageNum = 1, pageSize = 20, enableStatus } = params
  const query = { keyword, pageNum, pageSize }
  if (enableStatus !== undefined && enableStatus !== null && enableStatus !== '') {
    query.enableStatus = enableStatus
  }
  return request.get('/global-skills', { params: query }).then((body) => {
    const data = body.data || {}
    const rows = data.records || data.list || []
    return {
      list: rows,
      total: data.total || 0
    }
  })
}

export function createGlobalSkill(data) {
  return request.post('/global-skills', data).then((body) => body.data)
}

export function updateGlobalSkill(bizNo, data) {
  return request.put(`/global-skills/${bizNo}`, data)
}

export function deleteGlobalSkill(bizNo) {
  return request.delete(`/global-skills/${bizNo}`)
}