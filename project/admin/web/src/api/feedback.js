import request from '@/utils/request.js'

export function pageFeedbacks(params = {}) {
  return request.get('/api/v1/admin/feedbacks', { params }).then((body) => {
    const data = body.data || {}
    return {
      list: data.list || [],
      total: data.total || 0,
      page: data.page || 1,
      size: data.size || 20
    }
  })
}

export function getFeedback(id) {
  return request.get(`/api/v1/admin/feedbacks/${id}`).then((body) => body.data)
}

export function replyFeedback(id, data) {
  return request.post(`/api/v1/admin/feedbacks/${id}/reply`, data).then((body) => body.data)
}
