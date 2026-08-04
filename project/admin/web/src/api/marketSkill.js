import request from '@/utils/request.js'

export function listMarketSkills(params = {}) {
  const { keyword = '', pageNum = 1, pageSize = 20, enableStatus } = params
  const query = { keyword, pageNum, pageSize }
  if (enableStatus !== undefined && enableStatus !== null && enableStatus !== '') {
    query.enableStatus = enableStatus
  }
  return request.get('/market-skills', { params: query }).then((body) => {
    const data = body.data || {}
    const rows = data.records || data.list || []
    return {
      list: rows,
      total: data.total || 0
    }
  })
}

export function createMarketSkill(data) {
  return request.post('/market-skills', data).then((body) => body.data)
}

export function updateMarketSkill(bizNo, data) {
  return request.put(`/market-skills/${bizNo}`, data)
}

export function deleteMarketSkill(bizNo) {
  return request.delete(`/market-skills/${bizNo}`)
}
