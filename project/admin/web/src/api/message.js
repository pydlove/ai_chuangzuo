import request from '@/utils/request.js'

export function listMessages(params = {}) {
  return request.get('/api/v1/admin/messages', { params }).then((body) => {
    const data = body.data || {}
    return {
      list: data.list || [],
      total: data.total || 0
    }
  })
}

export function getMessageDetail(id) {
  return request.get(`/api/v1/admin/messages/${id}`).then((body) => body.data)
}

export function createMessage(data) {
  return request.post('/api/v1/admin/messages', data).then((body) => body.data)
}

export function updateMessage(id, data) {
  return request.put(`/api/v1/admin/messages/${id}`, data).then((body) => body.data)
}
