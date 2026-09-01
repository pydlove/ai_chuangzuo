import request from '@/utils/request.js'

export function getMarketSkill(bizNo) {
  return request.get(`/market-skills/${bizNo}`).then((body) => body.data)
}

export function listMarketSkills(params = {}) {
  const { keyword = '', pageNum = 1, pageSize = 20, enableStatus, featured } = params
  const query = { keyword, pageNum, pageSize }
  if (enableStatus !== undefined && enableStatus !== null && enableStatus !== '') {
    query.enableStatus = enableStatus
  }
  if (featured !== undefined && featured !== null && featured !== '') {
    query.featured = featured
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

export function batchDeleteMarketSkills(bizNos) {
  return request.post('/market-skills/batch-delete', { bizNos })
}

export function getMarketSkillStats() {
  return request.get('/market-skills/stats').then((body) => body.data || {})
}

export function getMarketSkillUsageRecords(bizNo, params = {}) {
  const { pageNum = 1, pageSize = 20 } = params
  return request.get(`/market-skills/${bizNo}/usage-records`, { params: { pageNum, pageSize } })
    .then((body) => {
      const data = body.data || {}
      const rows = data.items || []
      return {
        list: rows,
        total: data.total || 0,
        page: data.page || pageNum,
        size: data.size || pageSize
      }
    })
}

export function simulateMarketSkillUsage(bizNo, userId) {
  return request.post(`/market-skills/${bizNo}/simulate-usage`, { userId })
    .then((body) => body.data)
}
