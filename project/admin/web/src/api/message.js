import request from '@/utils/request.js'

export function listMessages(params = {}) {
  return request.get('/messages', { params }).then((body) => {
    const data = body.data || {}
    return {
      list: data.list || [],
      total: data.total || 0
    }
  })
}

export function getMessageDetail(id) {
  return request.get(`/messages/${id}`).then((body) => body.data)
}

export function createMessage(data) {
  return request.post('/messages', data).then((body) => body.data)
}

export function updateMessage(id, data) {
  return request.put(`/messages/${id}`, data).then((body) => body.data)
}
