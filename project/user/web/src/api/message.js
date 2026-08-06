import request from '@/utils/request'

export function getMessages() {
  return request.get('/messages')
}

export function getUnreadCount() {
  return request.get('/messages/unread-count')
}

export function markMessageRead(id) {
  return request.put(`/messages/${id}/read`)
}

export function markAllMessagesRead() {
  return request.put('/messages/read-all')
}
