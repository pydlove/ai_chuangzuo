import request from '@/utils/request'

export function submitFeedback(data) {
  return request.post('/feedback/submit', data)
}
