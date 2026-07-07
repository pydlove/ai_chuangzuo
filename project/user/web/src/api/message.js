import request from '@/utils/request'

export function getMessages() {
  return request.get('/messages')
}

export function markMessageRead(id) {
  return request.put(`/messages/${id}/read`)
}

export function markAllMessagesRead() {
  return request.put('/messages/read-all')
}
